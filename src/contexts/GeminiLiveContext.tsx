import React, { createContext, useContext, useRef } from "react";
import { GeminiLive } from "../services/geminiLive";
import { useAuth } from "./AuthContext";

type GeminiContextType = {
  live: GeminiLive | null;
  startLive: (listener: any) => Promise<{ chatId: string; live: GeminiLive }>;
  stopLive: () => Promise<void>;
};

const GeminiContext = createContext<GeminiContextType>({
  live: null,
  startLive: async (listener: any) => {
    return { chatId: "", live: (null as unknown) as GeminiLive };
  },
  stopLive: async () => { },
});

export const GeminiProvider: React.FC<{ children: React.ReactNode }> = ({
  children,
}) => {
  const { user } = useAuth();
  const liveRef = useRef<GeminiLive | null>(null);

  const startLive = async (listener: any): Promise<{ chatId: string; live: GeminiLive }> => {
    console.log("REAL startLive called!");
    console.log("GeminiProvider.startLive: user =", user?.id);
    if (!user?.id) {
      console.error("startLive: user not logged in", user);
      throw new Error("User not logged in");
    }
    console.log("Creating GeminiLive instance for user:", user.id);
    const liveInstance = new GeminiLive(user.id);
    console.log("Calling liveInstance.startLive()...");
    const chatId = await liveInstance.startLive(listener);
    console.log("liveInstance.startLive() returned chatId:", chatId);
    liveRef.current = liveInstance;
    return { chatId, live: liveInstance };
  };

  const stopLive = async () => {
    await liveRef.current?.stop();
    liveRef.current = null;
  };

  console.log("GeminiProvider rendered, user:", user?.id);

  return (
    <GeminiContext.Provider
      value={{ live: liveRef.current, startLive, stopLive }}
    >
      {children}
    </GeminiContext.Provider>
  );
};

export const useGeminiLive = () => useContext(GeminiContext);