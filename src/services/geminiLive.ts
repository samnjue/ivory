import { GoogleGenerativeAI } from "@google/generative-ai";
import { supabase } from "../utils/supabase";
import { Platform } from "react-native";
import { Audio } from "expo-av";

const GEMINI_API_KEY = "AIzaSyBVBUbRke_aAwNiwGnu5CrxzbCRWkHrrrE";

const genAI = new GoogleGenerativeAI(GEMINI_API_KEY);
const model = genAI.getGenerativeModel({ model: "gemini-2.5-flash-live" });

type Listener = {
    onCaption: (text: string) => void;
    onMessage: (role: "user" | "assistant", text: string) => void;
    onTitle: (title: string) => void;
    onEnd: () => void;
    onAmplitude?: (amplitude: number) => void;
};

export class GeminiLive {
    private chat?: any;
    private convId?: string;
    private convTitle?: string;
    private listener?: Listener;
    private audioRecording?: Audio.Recording;
    private isRecording = false;

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
                outputFormat: Audio.AndroidOutputFormat.AAC_ADIF,
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
                generationConfig: { responseMimeType: "text/plain" },
            });

            this.chat.on("content", async (part: any) => {
                const text = part.text ?? "";
                if (text) {
                    this.listener?.onCaption(text);
                    if (!this.convTitle && text.length > 30) {
                        const title = text.split("\n")[0].slice(0, 60).trim();
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

                const { sound } = await Audio.Sound.createAsync(
                { uri },
                { isLooping: false },
                null,
                false
                );
                const status = await sound.getStatusAsync();
                if (status.isLoaded && status.durationMillis) {
                const blob = await fetch(uri).then((r) => r.blob());
                await this.chat.sendMessage([{ inlineData: { mimeType: "audio/wav", data: blob } }]);
                }
            } catch (e) {
                console.warn("Audio chunk send failed", e);
            } finally {
                setTimeout(sendAudioChunk, 800);
            }
        };
        sendAudioChunk();
    }

    async stopMicStreaming() {
        if (!this.isRecording) return;
        await this.audioRecording?.stopAndUnloadAsync();
        this.audioRecording = undefined;
        this.isRecording = false;
    }

    async startLive(listener: Listener) {
        this.listener = listener;

        const { data: conv, error } = await supabase
            .from("conversations")
            .insert({ user_id: this.userId })
            .select()
            .single();
        
        if (error) throw error;
        this.convId = conv.id;

        this.chat = model.startChat({
        generationConfig: { responseMimeType: "text/plain" },
        });

        this.chat.on("content", async (part: any) => {
        const text = part.text ?? "";
        if (text) {
            listener.onCaption(text);
            if (!conv.title && text.length > 30) {
            const title = text.split("\n")[0].slice(0, 60).trim();
            await this.updateTitle(title);
            listener.onTitle(title);
            }
        }
        });

        await this.sendUserMessage("");
    }

    async sendUserMessage(text: string) {
        if (!this.chat || !this.convId) return;

        const { error: msgErr } = await supabase
        .from("messages")
        .insert({
            conversation_id: this.convId,
            role: "user",
            content: text,
        });

        if (msgErr) console.error(msgErr);

        this.listener?.onMessage("user", text);

        const result = await this.chat.sendMessageStream(text);
        let assistant = "";
        for await (const chunk of result.stream) {
        const chunkText = chunk.text();
        assistant += chunkText;
        this.listener?.onCaption(chunkText);
        }

        await supabase
        .from("messages")
        .insert({
            conversation_id: this.convId,
            role: "assistant",
            content: assistant,
        });

        this.listener?.onMessage("assistant", assistant);
    }

    private async updateTitle(title: string) {
        if (!this.convId) return;
        await supabase
        .from("conversations")
        .update({ title })
        .eq("id", this.convId);
    }

    async stop() {
        this.chat = undefined;
        this.listener?.onEnd();
    }
}