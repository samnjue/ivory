import React, { createContext, useState, useEffect, useContext } from 'react';
import { supabase } from '../utils/supabase';
import { Session, User } from '@supabase/supabase-js';
import { Alert } from 'react-native';
import * as WebBrowser from 'expo-web-browser';
import * as AuthSession from 'expo-auth-session';

interface AuthContextType {
  session: Session | null;
  user: User | undefined;
  signUp: (email: string, password: string, fullName?: string) => Promise<void>;
  signIn: (email: string, password: string) => Promise<void>;
  signInWithGoogle: () => Promise<void>;
  signOut: () => Promise<void>;
  resetPassword: (email: string) => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [session, setSession] = useState<Session | null>(null);
  const [user, setUser] = useState<User | undefined>(undefined);

  useEffect(() => {
    supabase.auth.getSession().then(({ data: { session } }) => {
      setSession(session);
      setUser(session?.user);
    });

    const { data: { subscription } } = supabase.auth.onAuthStateChange((_event, session) => {
      setSession(session);
      setUser(session?.user);
    });

    return () => subscription.unsubscribe();
  }, []);

  const signUp = async (email: string, password: string, fullName?: string) => {
    try {
      const { data, error } = await supabase.auth.signUp({
        email,
        password,
        options: { data: { full_name: fullName } },
      });
      if (error) throw error;
    } catch (error: any) {
      Alert.alert('Sign Up Error', error.message);
      throw error;
    }
  };

  const signIn = async (email: string, password: string) => {
    try {
      const { data, error } = await supabase.auth.signInWithPassword({ email, password });
      if (error) throw error;
    } catch (error: any) {
      Alert.alert('Sign In Error', error.message);
      throw error;
    }
  };

  WebBrowser.maybeCompleteAuthSession(); 

    const signInWithGoogle = async () => {
    try {
        const redirectUri = AuthSession.makeRedirectUri({ native: 'ivory://login-callback' });
        const { data, error } = await supabase.auth.signInWithOAuth({
        provider: 'google',
        options: {
            redirectTo: redirectUri,
            skipBrowserRedirect: true, 
        },
        });
        if (error) throw error;
    } catch (error: any) {
        Alert.alert('Google Sign In Error', error.message);
        throw error;
    }
    };

  const signOut = async () => {
    try {
      const { error } = await supabase.auth.signOut();
      if (error) throw error;
    } catch (error: any) {
      Alert.alert('Sign Out Error', error.message);
      throw error;
    }
  };

  const resetPassword = async (email: string) => {
    try {
      const { error } = await supabase.auth.resetPasswordForEmail(email);
      if (error) throw error;
      Alert.alert('Success', 'Password reset email sent!');
    } catch (error: any) {
      Alert.alert('Reset Error', error.message);
      throw error;
    }
  };

  return (
    <AuthContext.Provider value={{ session, user, signUp, signIn, signInWithGoogle, signOut, resetPassword }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within AuthProvider');
  return context;
};