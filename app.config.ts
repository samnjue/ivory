import 'dotenv/config';
import { ExpoConfig } from '@expo/config-types';

const config = ({
  config,
}: {
  config: ExpoConfig;
}): ExpoConfig => {
  return {
    ...config,
    extra: {
      supabaseUrl: process.env.SUPABASE_PROJECT_URL,
      supabaseAnonKey: process.env.SUPABASE_API_KEY,
      geminiApiKey: process.env.GEMINI_API_KEY,
    },
  };
};

export default config;