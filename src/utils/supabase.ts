import { createClient } from "@supabase/supabase-js";
import AsyncStorage from "@react-native-async-storage/async-storage";
import * as SecureStore from "expo-secure-store";
import Constants from "expo-constants";

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

const supabaseUrl = Constants.expoConfig?.extra?.supabaseUrl;
const supabaseAnonKey = Constants.expoConfig?.extra?.supabaseAnonKey;

if (!supabaseUrl || !supabaseAnonKey) {
  throw new Error('Supabase URL and Anon Key are required. Check app.config.ts and .env');
}

export const supabase = createClient(supabaseUrl, supabaseAnonKey, {
	auth: {
		storage: hybridStorage,
		autoRefreshToken: true,
		persistSession: true,
		detectSessionInUrl: true,
	},
});
