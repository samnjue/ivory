import { GoogleGenerativeAI } from "@google/generative-ai";
import { supabase } from "../utils/supabase";
import { Platform } from "react-native";

const GEMINI_API_KEY = "AIzaSyBVBUbRke_aAwNiwGnu5CrxzbCRWkHrrrE";

const genAI = new GoogleGenerativeAI(GEMINI_API_KEY);
const model = genAI.getGenerativeModel({ model: "gemini-2.5-flash-live" });

type Listener = {
    onCaption: (text: string) => void;
    onMessage: (role: "user" | "assistant", text: string) => void;
    onTitle: (title: string) => void;
    onEnd: () => void;
};

export class GeminiLive {
    private chat?: any;
    private convId?: string;
    private listener?: Listener;

    constructor(private userId: string) {}

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