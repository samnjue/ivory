import { supabase } from "../utils/supabase";

//Types & Interfaces
export interface Chat {
	id: string;
	user_id: string;
	title: string | null;
	created_at: string;
	updated_at: string;
	is_live: boolean;
}

export interface Message {
	id: string;
	chat_id: string;
	role: "user" | "assistant";
	content: string;
	created_at: string;
	metadata?: {
		is_caption?: boolean;
		audio_duration?: number;
	};
}

export interface Caption {
	id: string;
	chat_id: string;
	text: string;
	timestamp: number;
	created_at: string;
}

export interface GroupedChats {
	today?: Chat[];
	yesterday?: Chat[];
	thisWeek?: Chat[];
	lastWeek?: Chat[];
	[monthKey: string]: Chat[] | undefined;
}

// API Service
export class ChatAPI {
	//Chat Management
	static async createChat(userId: string): Promise<Chat> {
		const { data, error } = await supabase
			.from("chats")
			.insert({
				user_id: userId,
				title: null,
				is_live: true,
				created_at: new Date().toISOString(),
				updated_at: new Date().toISOString(),
			})
			.select()
			.single();

		if (error) throw new Error(`Failed to create chat: ${error.message}`);
		return data;
	}

	static async updateChatTitle(chatId: string, title: string): Promise<void> {
		const { error } = await supabase
			.from("chats")
			.update({
				title: title,
				updated_at: new Date().toISOString(),
			})
			.eq("id", chatId);

		if (error) throw new Error(`Failed to update chat title: ${error.message}`);
	}

	static async updateChatLiveStatus(
		chatId: string,
		isLive: boolean
	): Promise<void> {
		const { error } = await supabase
			.from("chats")
			.update({
				is_live: isLive,
				updated_at: new Date().toISOString(),
			})
			.eq("id", chatId);

		if (error)
			throw new Error(`Failed to update chat status: ${error.message}`);
	}

	static async fetchRecentChats(userId: string): Promise<GroupedChats> {
		const { data, error } = await supabase
			.from("chats")
			.select("*")
			.eq("user_id", userId)
			.order("updated_at", { ascending: false })
			.limit(100);

		if (error) throw new Error(`Failed to fetch chats: ${error.message}`);

		return this.groupChatsByDate(data);
	}

	private static groupChatsByDate(chats: Chat[]): GroupedChats {
		const now = new Date();
		const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
		const yesterday = new Date(today);
		yesterday.setDate(yesterday.getDate() - 1);

		const thisWeekStart = new Date(today);
		thisWeekStart.setDate(today.getDate() - today.getDay()); 

		const lastWeekStart = new Date(thisWeekStart);
		lastWeekStart.setDate(lastWeekStart.getDate() - 7);

		const grouped: GroupedChats = {};
		const dayNames = [
			"Sunday",
			"Monday",
			"Tuesday",
			"Wednesday",
			"Thursday",
			"Friday",
			"Saturday",
		];
		const monthNames = [
			"January",
			"February",
			"March",
			"April",
			"May",
			"June",
			"July",
			"August",
			"September",
			"October",
			"November",
			"December",
		];

		chats.forEach((chat) => {
			const chatDate = new Date(chat.updated_at);
			const chatDay = new Date(
				chatDate.getFullYear(),
				chatDate.getMonth(),
				chatDate.getDate()
			);

			if (chatDay.getTime() === today.getTime()) {
				if (!grouped.today) grouped.today = [];
				grouped.today.push(chat);
			} else if (chatDay.getTime() === yesterday.getTime()) {
				if (!grouped.yesterday) grouped.yesterday = [];
				grouped.yesterday.push(chat);
			} else if (chatDate >= thisWeekStart) {
				const dayName = dayNames[chatDate.getDay()];
				if (!grouped[dayName]) grouped[dayName] = [];
				grouped[dayName]!.push(chat);
			} else if (chatDate >= lastWeekStart) {
				if (!grouped.lastWeek) grouped.lastWeek = [];
				grouped.lastWeek.push(chat);
			} else {
				const monthKey = monthNames[chatDate.getMonth()];
				if (!grouped[monthKey]) grouped[monthKey] = [];
				grouped[monthKey]!.push(chat);
			}
		});

		return grouped;
	}

	static async getChatById(chatId: string): Promise<Chat> {
		const { data, error } = await supabase
			.from("chats")
			.select("*")
			.eq("id", chatId)
			.single();

		if (error) throw new Error(`Failed to fetch chat: ${error.message}`);
		return data;
	}

	static async addMessage(
		chatId: string,
		role: "user" | "assistant",
		content: string,
		metadata?: Message["metadata"]
	): Promise<Message> {
		const { data, error } = await supabase
			.from("messages")
			.insert({
				chat_id: chatId,
				role,
				content,
				metadata,
				created_at: new Date().toISOString(),
			})
			.select()
			.single();

		if (error) throw new Error(`Failed to add message: ${error.message}`);

		await this.updateChatLiveStatus(chatId, true);

		return data;
	}

	static async fetchMessages(chatId: string): Promise<Message[]> {
		const { data, error } = await supabase
			.from("messages")
			.select("*")
			.eq("chat_id", chatId)
			.order("created_at", { ascending: true });

		if (error) throw new Error(`Failed to fetch messages: ${error.message}`);
		return data;
	}

	static subscribeToMessages(
		chatId: string,
		callback: (message: Message) => void
	) {
		return supabase
			.channel(`messages:${chatId}`)
			.on(
				"postgres_changes",
				{
					event: "INSERT",
					schema: "public",
					table: "messages",
					filter: `chat_id=eq.${chatId}`,
				},
				(payload) => {
					callback(payload.new as Message);
				}
			)
			.subscribe();
	}

    //Caption Management
	static async addCaption(
		chatId: string,
		text: string,
		timestamp: number
	): Promise<Caption> {
		const { data, error } = await supabase
			.from("captions")
			.insert({
				chat_id: chatId,
				text,
				timestamp,
				created_at: new Date().toISOString(),
			})
			.select()
			.single();

		if (error) throw new Error(`Failed to add caption: ${error.message}`);
		return data;
	}

	static async fetchCaptions(chatId: string): Promise<Caption[]> {
		const { data, error } = await supabase
			.from("captions")
			.select("*")
			.eq("chat_id", chatId)
			.order("timestamp", { ascending: true });

		if (error) throw new Error(`Failed to fetch captions: ${error.message}`);
		return data;
	}

	static subscribeToCaptions(
		chatId: string,
		callback: (caption: Caption) => void
	) {
		return supabase
			.channel(`captions:${chatId}`)
			.on(
				"postgres_changes",
				{
					event: "INSERT",
					schema: "public",
					table: "captions",
					filter: `chat_id=eq.${chatId}`,
				},
				(payload) => {
					callback(payload.new as Caption);
				}
			)
			.subscribe();
	}
}

// Gemini AI Integration
const GEMINI_API_KEY = "AIzaSyBVBUbRke_aAwNiwGnu5CrxzbCRWkHrrrE";

export class GeminiService {

	static async generateChatTitle(messages: Message[]): Promise<string> {
		const contextMessages = messages.slice(0, 6);
		const conversationText = contextMessages
			.map((m) => `${m.role === "user" ? "User" : "Assistant"}: ${m.content}`)
			.join("\n");

		try {
			const response = await fetch(
				`https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=${GEMINI_API_KEY}`,
				{
					method: "POST",
					headers: {
						"Content-Type": "application/json",
					},
					body: JSON.stringify({
						contents: [
							{
								parts: [
									{
										text: `Based on this conversation, generate a short, descriptive title (maximum 5 words):\n\n${conversationText}\n\nRespond with ONLY the title, no quotes or extra text.`,
									},
								],
							},
						],
						generationConfig: {
							temperature: 0.7,
							maxOutputTokens: 50,
						},
					}),
				}
			);

			const data = await response.json();

			if (data.candidates && data.candidates[0]?.content?.parts?.[0]?.text) {
				return data.candidates[0].content.parts[0].text.trim();
			}

			return "New Conversation";
		} catch (error) {
			console.error("Failed to generate title:", error);
			return "New Conversation";
		}
	}

	static async getResponse(
		chatId: string,
		userMessage: string,
		conversationHistory: Message[] = []
	): Promise<string> {
		try {
			const contents = conversationHistory.map((msg) => ({
				role: msg.role === "user" ? "user" : "model",
				parts: [{ text: msg.content }],
			}));

			contents.push({
				role: "user",
				parts: [{ text: userMessage }],
			});

			const response = await fetch(
				`https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=${GEMINI_API_KEY}`,
				{
					method: "POST",
					headers: {
						"Content-Type": "application/json",
					},
					body: JSON.stringify({
						contents,
						generationConfig: {
							temperature: 0.9,
							maxOutputTokens: 2048,
							topP: 1,
							topK: 1,
						},
					}),
				}
			);

			const data = await response.json();

			if (data.candidates && data.candidates[0]?.content?.parts?.[0]?.text) {
				return data.candidates[0].content.parts[0].text;
			}

			throw new Error("No response from Gemini API");
		} catch (error) {
			console.error("Failed to get Gemini response:", error);
			throw error;
		}
	}

	static async processStreamingResponse(
		chatId: string,
		userMessage: string,
		onCaption: (text: string) => void,
		onComplete: (fullResponse: string) => void
	): Promise<void> {
		let fullResponse = "";
		let captionBuffer = "";
		const CAPTION_CHUNK_SIZE = 50;

		try {
			const response = await fetch(
				`https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:streamGenerateContent?key=${GEMINI_API_KEY}&alt=sse`,
				{
					method: "POST",
					headers: {
						"Content-Type": "application/json",
					},
					body: JSON.stringify({
						contents: [
							{
								parts: [
									{
										text: userMessage,
									},
								],
							},
						],
						generationConfig: {
							temperature: 0.9,
							maxOutputTokens: 2048,
						},
					}),
				}
			);

			const reader = response.body?.getReader();
			const decoder = new TextDecoder();

			while (true) {
				const { done, value } = await reader!.read();
				if (done) break;

				const chunk = decoder.decode(value);
				const lines = chunk.split("\n");

				for (const line of lines) {
					if (line.startsWith("data: ")) {
						try {
							const jsonStr = line.slice(6);
							if (jsonStr.trim() === "[DONE]") continue;

							const data = JSON.parse(jsonStr);

							if (data.candidates?.[0]?.content?.parts?.[0]?.text) {
								const text = data.candidates[0].content.parts[0].text;
								fullResponse += text;
								captionBuffer += text;

								const words = captionBuffer.split(" ");
								if (words.length >= CAPTION_CHUNK_SIZE) {
									const captionText = words
										.slice(0, CAPTION_CHUNK_SIZE)
										.join(" ");
									onCaption(captionText);
									await ChatAPI.addCaption(chatId, captionText, Date.now());
									captionBuffer = words.slice(CAPTION_CHUNK_SIZE).join(" ");
								}
							}
						} catch (parseError) {
							continue;
						}
					}
				}
			}

			if (captionBuffer.trim()) {
				onCaption(captionBuffer);
				await ChatAPI.addCaption(chatId, captionBuffer, Date.now());
			}

			await ChatAPI.addMessage(chatId, "assistant", fullResponse);
			onComplete(fullResponse);
		} catch (error) {
			console.error("Streaming error:", error);
			throw error;
		}
	}
}
