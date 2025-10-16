import { createClient } from '@supabase/supabase-js';
import AsyncStorage from '@react-native-async-storage/async-storage';
import * as SecureStore from 'expo-secure-store';

const secureStorage = {
  getItem: async (key: string) => {
    try {
      return await SecureStore.getItemAsync(key);
    } catch {
      return await AsyncStorage.getItem(key);
    }
  },
  setItem: async (key: string, value: string) => {
    try {
      await SecureStore.setItemAsync(key, value);
    } catch {
      await AsyncStorage.setItem(key, value);
    }
  },
  removeItem: async (key: string) => {
    try {
      await SecureStore.deleteItemAsync(key);
    } catch {
      await AsyncStorage.removeItem(key);
    }
  },
};

const supabaseUrl = "https://jznkrzfeyjcalqblgzri.supabase.co";
const supabaseAnonKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imp6bmtyemZleWpjYWxxYmxnenJpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjA0MjM5MDUsImV4cCI6MjA3NTk5OTkwNX0.62AjwddpnQU-ydVdLb1BygCxNDRUFnIUyZCZzk2QgRk";

export const supabase = createClient(supabaseUrl, supabaseAnonKey, {
  auth: {
    storage: secureStorage,
    autoRefreshToken: true,
    persistSession: true,
    detectSessionInUrl: true,
  },
});