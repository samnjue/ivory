import { GoogleGenerativeAI } from "@google/generative-ai";
import { supabase } from "../utils/supabase";
import { Audio } from "expo-av";
import { useAuth } from "../contexts/AuthContext";

const GEMINI_API_KEY = "AIzaSyBVBUbRke_aAwNiwGnu5CrxzbCRWkHrrrE";

const genAI = new GoogleGenerativeAI(GEMINI_API_KEY);
const model = genAI.getGenerativeModel({ model: "gemini-2.5-flash-live" });

console.log("Gemini model loaded:", model);

type Listener = {
	onCaption: (text: string) => void;
	onMessage: (text: string) => void;
	onTitle: (title: string) => void;
	onEnd: () => void;
	onAmplitude?: (amplitude: number) => void;
};

export class GeminiLive {
	private chat?: any;
	private chatId?: string;
	private chatTitle?: string;
	private listener?: Listener;
	private audioRecording?: Audio.Recording;
	private isRecording = false;
	private assistantBuffer = "";
	private soundObject?: Audio.Sound;

	constructor(private userId: string) {}

	async startMicStreaming() {
		if (this.isRecording) return;

		const { status } = await Audio.requestPermissionsAsync();
		if (status !== "granted") throw new Error("Microphone permission denied");

		await Audio.setAudioModeAsync({
			allowsRecordingIOS: true,
			playsInSilentModeIOS: true,
		});

		const rec = new Audio.Recording();
		await rec.prepareToRecordAsync({
			android: {
				extension: ".wav",
				sampleRate: 16000,
				numberOfChannels: 1,
				outputFormat: 0,
				audioEncoder: 0,
			},
			ios: {
				extension: ".wav",
				sampleRate: 16000,
				numberOfChannels: 1,
				audioQuality: 0,
				bitRate: 0,
			},
			web: {
				mimeType: undefined,
				bitsPerSecond: undefined,
			},
		});

		rec.setOnRecordingStatusUpdate((s) => {
			if (s.isRecording && s.metering != null) {
				const amp = Math.max(0, (s.metering + 60) / 60);
				this.listener?.onAmplitude?.(amp);
			}
		});

		await rec.startAsync();
		this.audioRecording = rec;
		this.isRecording = true;

		const sendChunk = async () => {
			if (!this.isRecording || !this.audioRecording || !this.chat) {
				setTimeout(sendChunk, 1000);
				return;
			}

			try {
				const uri = this.audioRecording.getURI();
				if (!uri) {
					setTimeout(sendChunk, 1000);
					return;
				}

				const resp = await fetch(uri);
				const blob = await resp.blob();
				const reader = new FileReader();

				reader.onloadend = async () => {
					const base64 = (reader.result as string).split(",")[1];

					try {
						const result = await this.chat.sendMessageStream([
							{ inlineData: { mimeType: "audio/wav", data: base64 } },
						]);

						for await (const chunk of result.stream) {
							const text = chunk.text();
							if (text) {
								this.listener?.onCaption(text);
								this.assistantBuffer += text;

								if (this.chatId) {
									await supabase.from("captions").insert({
										chat_id: this.chatId,
										text,
										timestamp: Date.now(),
									});
								}

								if (!this.chatTitle && this.assistantBuffer.length > 50) {
									const title = this.assistantBuffer
										.split("\n")[0]
										.slice(0, 60)
										.trim();
									await this.updateTitle(title);
									this.listener?.onTitle(title);
								}
							}

							const audioPart =
								chunk.candidates?.[0]?.content?.parts?.[0]?.inlineData;
							if (audioPart?.data) {
								await this.playAudio(audioPart.data);
							}
						}

						this.listener?.onMessage(this.assistantBuffer);
						await supabase.from("messages").insert({
							chat_id: this.chatId!,
							role: "assistant",
							content: this.assistantBuffer,
						});
						this.assistantBuffer = ""; 
					} catch (streamError) {
						console.error("Stream error:", streamError);
					}
				};

				reader.readAsDataURL(blob);
			} catch (e) {
				console.warn("sendChunk error:", e);
			} finally {
				setTimeout(sendChunk, 1000);
			}
		};

		sendChunk();
	}

	async stopMicStreaming() {
		if (!this.isRecording) return;

		if (this.assistantBuffer && this.chatId) {
			await supabase.from("messages").insert({
				chat_id: this.chatId,
				role: "assistant",
				content: this.assistantBuffer,
			});
			this.listener?.onMessage(this.assistantBuffer);
		}

		await this.audioRecording?.stopAndUnloadAsync();
		this.audioRecording = undefined;
		this.isRecording = false;
		this.assistantBuffer = "";
	}

	async startLive(
		listener: Listener,
		existingChatId?: string
	): Promise<string> {
        console.log("GeminiLive.startLive called for user:", this.userId);
		this.listener = listener;

		let chatId: string;
		if (existingChatId) {
			chatId = existingChatId;
			const { data } = await supabase
				.from("chats")
				.select("*")
				.eq("id", existingChatId)
				.single();
			this.chatTitle = data?.title ?? undefined;
			await supabase
				.from("chats")
				.update({ is_live: true, updated_at: new Date().toISOString() })
				.eq("id", existingChatId);
		} else {
            console.log("Inserting new chat into Supabase...");
			const { data, error } = await supabase
				.from("chats")
				.insert({
					user_id: this.userId,
					title: null,
					is_live: true,
					created_at: new Date().toISOString(),
					updated_at: new Date().toISOString(),
				})
				.select()
				.single();

			console.log("Supabase insert response:", { data, error });

			if (error || !data) {
				console.error("Supabase insert failed:", error);
				throw new Error(
					`Failed to create chat: ${error?.message || "No data returned"}`
				);
			}
			chatId = data.id;
		}

		this.chatId = chatId;

        console.log("Starting Gemini chat session...");
		this.chat = model.startChat({
			history: [],
			generationConfig: {
				responseMimeType: "audio/wav",
				temperature: 0.9,
			},
		});

		console.log("Chat session started:", !!this.chat);

		return chatId;
	}

	private async playAudio(base64: string) {
		try {
			await this.soundObject?.unloadAsync();

			const uri = `data:audio/wav;base64,${base64}`;
			const { sound } = await Audio.Sound.createAsync(
				{ uri },
				{ shouldPlay: true }
			);
			this.soundObject = sound;
			await sound.playAsync();
		} catch (e) {
			console.warn("playAudio error", e);
		}
	}

	async sendUserMessage(text: string) {
		if (!this.chat || !this.chatId) return;

		const { error: msgErr } = await supabase.from("messages").insert({
			chat_id: this.chatId,
			role: "user",
			content: text,
		});

		if (msgErr) console.error("Failed to save user message:", msgErr);

		const result = await this.chat.sendMessageStream(text);
		let assistant = "";

		for await (const chunk of result.stream) {
			const chunkText = chunk.text();
			assistant += chunkText;
			this.listener?.onCaption(chunkText);

			if (this.chatId) {
				await supabase.from("captions").insert({
					chat_id: this.chatId,
					text: chunkText,
					timestamp: Date.now(),
				});
			}
		}

		await supabase.from("messages").insert({
			chat_id: this.chatId,
			role: "assistant",
			content: assistant,
		});

		this.listener?.onMessage(assistant);

		if (!this.chatTitle && assistant.length > 50) {
			const title = assistant.split("\n")[0].slice(0, 60).trim();
			await this.updateTitle(title);
			this.listener?.onTitle(title);
		}
	}

	private async updateTitle(title: string) {
		if (!this.chatId || this.chatTitle) return;
		this.chatTitle = title;
		await supabase
			.from("chats")
			.update({ title, updated_at: new Date().toISOString() })
			.eq("id", this.chatId);
	}

	getChatId(): string | undefined {
		return this.chatId;
	}

	async stop() {
		if (this.assistantBuffer && this.chatId) {
			await supabase.from("messages").insert({
				chat_id: this.chatId,
				role: "assistant",
				content: this.assistantBuffer,
			});
		}

		if (this.chatId) {
			await supabase
				.from("chats")
				.update({ is_live: false, updated_at: new Date().toISOString() })
				.eq("id", this.chatId);
		}

		await this.soundObject?.unloadAsync();
		this.chat = undefined;
		this.assistantBuffer = "";
		this.listener?.onEnd();
	}
}
