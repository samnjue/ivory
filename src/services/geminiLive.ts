import { GoogleGenerativeAI } from "@google/generative-ai";
import { supabase } from "../utils/supabase";
import { Platform } from "react-native";
import { Audio } from "expo-av";

const GEMINI_API_KEY = "AIzaSyBVBUbRke_aAwNiwGnu5CrxzbCRWkHrrrE";

const genAI = new GoogleGenerativeAI(GEMINI_API_KEY);
const model = genAI.getGenerativeModel({ model: "gemini-2.0-flash-exp" });

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

    constructor(private userId: string) {}

    async startMicStreaming() {
        if (this.isRecording) return;

        const { status } = await Audio.requestPermissionsAsync();
        if (status !== "granted") throw new Error("Microphone permission denied");

        await Audio.setAudioModeAsync({
            allowsRecordingIOS: true,
            playsInSilentModeIOS: true,
        });

        const recording = new Audio.Recording();
        await recording.prepareToRecordAsync({
            android: {
                extension: ".wav",
                outputFormat: Audio.AndroidOutputFormat.DEFAULT,
                audioEncoder: Audio.AndroidAudioEncoder.DEFAULT,
                sampleRate: 16000,
                numberOfChannels: 1,
                bitRate: 64000,
            },
            ios: {
                extension: ".wav",
                audioQuality: Audio.IOSAudioQuality.HIGH,
                sampleRate: 16000,
                numberOfChannels: 1,
                bitRate: 64000,
                linearPCMBitDepth: 16,
                linearPCMIsBigEndian: false,
                linearPCMIsFloat: false,
            },
            web: {},
        });

        recording.setOnRecordingStatusUpdate((status) => {
            if (!status.isRecording || status.metering == null) return;

            const amplitude = Math.max(0, (status.metering + 60) / 60);
            this.listener?.onAmplitude?.(amplitude);
        });

        await recording.startAsync();
        this.audioRecording = recording;
        this.isRecording = true;

        if (!this.chat) {
            this.chat = model.startChat({
                generationConfig: { 
                    responseMimeType: "text/plain",
                    temperature: 0.9,
                },
            });

            this.chat.on("content", async (part: any) => {
                const text = part.text ?? "";
                if (text) {
                    this.listener?.onCaption(text);
                    this.assistantBuffer += text;

                    if (this.chatId) {
                        await supabase.from("captions").insert({
                            chat_id: this.chatId,
                            text: text,
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
            });
        }

        const sendAudioChunk = async () => {
            if (!this.isRecording || !this.audioRecording) return;
            
            try {
                const uri = this.audioRecording.getURI();
                if (!uri) return;

                const response = await fetch(uri);
                const blob = await response.blob();

                const reader = new FileReader();
                reader.readAsDataURL(blob);
                reader.onloadend = async () => {
                    const base64data = reader.result as string;
                    const base64Audio = base64data.split(',')[1];

                    if (this.chat) {
                        await this.chat.sendMessage([
                            { 
                                inlineData: { 
                                    mimeType: "audio/wav", 
                                    data: base64Audio 
                                } 
                            }
                        ]);
                    }
                };
            } catch (e) {
                console.warn("Audio chunk send failed", e);
            } finally {
                setTimeout(sendAudioChunk, 1000);
            }
        };
        
        sendAudioChunk();
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

    async startLive(listener: Listener, existingChatId?: string) {
        this.listener = listener;

        if (existingChatId) {
            this.chatId = existingChatId;
 
            const { data: chat } = await supabase
                .from("chats")
                .select("*")
                .eq("id", existingChatId)
                .single();
            
            if (chat) {
                this.chatTitle = chat.title;
            }

            await supabase
                .from("chats")
                .update({ is_live: true, updated_at: new Date().toISOString() })
                .eq("id", existingChatId);
        } else {
            const { data: chat, error } = await supabase
                .from("chats")
                .insert({ 
                    user_id: this.userId,
                    is_live: true,
                    created_at: new Date().toISOString(),
                    updated_at: new Date().toISOString(),
                })
                .select()
                .single();

            if (error) {
                console.error("Failed to create chat:", error);
                throw error;
            }
            
            this.chatId = chat.id;
        }

        this.chat = model.startChat({
            generationConfig: { 
                responseMimeType: "text/plain",
                temperature: 0.9,
            },
        });

        this.chat.on("content", async (part: any) => {
            const text = part.text ?? "";
            if (text) {
                this.listener?.onCaption(text);
                this.assistantBuffer += text;

                if (this.chatId) {
                    await supabase.from("captions").insert({
                        chat_id: this.chatId,
                        text: text,
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
        });
    }

    async sendUserMessage(text: string) {
        if (!this.chat || !this.chatId) return;

        const { error: msgErr } = await supabase
            .from("messages")
            .insert({
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
            .update({ 
                title,
                updated_at: new Date().toISOString() 
            })
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
                .update({ 
                    is_live: false,
                    updated_at: new Date().toISOString() 
                })
                .eq("id", this.chatId);
        }

        this.chat = undefined;
        this.assistantBuffer = "";
        this.listener?.onEnd();
    }
}