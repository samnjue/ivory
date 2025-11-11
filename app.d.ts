declare namespace NodeJS {
  interface ProcessEnv {
    SUPABASE_PROJECT_URL: string;
    SUPABASE_API_KEY: string;
    GEMINI_API_KEY: string;
  }
}