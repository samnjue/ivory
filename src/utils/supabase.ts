import { createClient } from "@supabase/supabase-js";
import AsyncStorage from "@react-native-async-storage/async-storage";
import * as SecureStore from "expo-secure-store";

const MAX_SECURESTORE_SIZE = 2048;

const hybridStorage = {
	getItem: async (key: string) => {
		try {
			const secureData = await SecureStore.getItemAsync(key);
			if (secureData) return secureData;
			const asyncData = await AsyncStorage.getItem(key);
			return asyncData;
		} catch (error) {
			console.error("Storage getItem error:", error);
			return null;
		}
	},
	setItem: async (key: string, value: string) => {
		try {
			const valueSize = new TextEncoder().encode(value).length;
			if (valueSize <= MAX_SECURESTORE_SIZE) {
				await SecureStore.setItemAsync(key, value);
			} else {
				console.warn(
					`Data for ${key} exceeds ${MAX_SECURESTORE_SIZE} bytes, stored in AsyncStorage`
				);
				await AsyncStorage.setItem(key, value);
			}
		} catch (error) {
			console.error("Storage setItem error:", error);
			throw error;
		}
	},
	removeItem: async (key: string) => {
		try {
			await SecureStore.deleteItemAsync(key).catch(() => {});
			await AsyncStorage.removeItem(key).catch(() => {});
		} catch (error) {
			console.error("Storage removeItem error:", error);
			throw error;
		}
	},
};

const supabaseUrl = "https://jznkrzfeyjcalqblgzri.supabase.co";
const supabaseAnonKey =
	"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imp6bmtyemZleWpjYWxxYmxnenJpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjA0MjM5MDUsImV4cCI6MjA3NTk5OTkwNX0.62AjwddpnQU-ydVdLb1BygCxNDRUFnIUyZCZzk2QgRk";

export const supabase = createClient(supabaseUrl, supabaseAnonKey, {
	auth: {
		storage: hybridStorage,
		autoRefreshToken: true,
		persistSession: true,
		detectSessionInUrl: true,
	},
});
