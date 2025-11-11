import React, { createContext, useContext, useRef } from "react";
import { GeminiLive } from "../services/geminLive";
import { useAuth } from "./AuthContext"; 

type GeminiContextType = {
  live: GeminiLive | null;
  startLive: (listener: any) => Promise<void>;
  stopLive: () => Promise<void>;
};

const GeminiContext = createContext<GeminiContextType>({
  live: null,
  startLive: async () => {},
  stopLive: async () => {},
});

export const GeminiProvider: React.FC<{ children: React.ReactNode }> = ({
  children,
}) => {
  const { user } = useAuth();
  const liveRef = useRef<GeminiLive | null>(null);

  const startLive = async (listener: any) => {
    if (!user) throw new Error("User not logged in");
    liveRef.current = new GeminiLive(user.id);
    await liveRef.current.startLive(listener);
  };

  const stopLive = async () => {
    await liveRef.current?.stop();
    liveRef.current = null;
  };

  return (
    <GeminiContext.Provider
      value={{ live: liveRef.current, startLive, stopLive }}
    >
      {children}
    </GeminiContext.Provider>
  );
};

export const useGeminiLive = () => useContext(GeminiContext);