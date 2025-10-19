import "react-native-gesture-handler";
import React, { useState, useEffect, useRef } from "react";
import AssistantOverlay from "./src/components/AssistantOverlay";
import { NavigationContainer } from "@react-navigation/native";
import * as SplashScreen from "expo-splash-screen";
import {
  useFonts,
  RedRose_400Regular,
  RedRose_500Medium,
  RedRose_600SemiBold,
  RedRose_700Bold,
} from "@expo-google-fonts/red-rose";
import { SafeAreaProvider } from "react-native-safe-area-context";
import { ThemeProvider } from "./src/contexts/ThemeContext";
import { AuthProvider, useAuth } from "./src/contexts/AuthContext";
import AuthNavigator from "./src/navigation/AuthNavigator";
import MainNavigator from "./src/navigation/MainNavigator";
import { supabase } from "./src/utils/supabase";
import * as Linking from "expo-linking";
import { createStackNavigator } from "@react-navigation/stack";
import {
  CardStyleInterpolators,
  TransitionSpecs,
} from "@react-navigation/stack";
import { DeviceEventEmitter, NativeModules } from 'react-native';

const { AssistantModule } = NativeModules;

const Stack = createStackNavigator();
SplashScreen.preventAutoHideAsync();

const RootNavigator = () => {
  const { session } = useAuth();

  return (
    <Stack.Navigator
      initialRouteName={session ? "Main" : "Auth"}
      screenOptions={{
        headerShown: false,
        cardStyleInterpolator: CardStyleInterpolators.forHorizontalIOS,
        transitionSpec: {
          open: TransitionSpecs.TransitionIOSSpec,
          close: TransitionSpecs.TransitionIOSSpec,
        },
      }}
    >
      {session ? (
        <Stack.Screen name="Main" component={MainNavigator} />
      ) : (
        <Stack.Screen name="Auth" component={AuthNavigator} />
      )}
    </Stack.Navigator>
  );
};

export default function App() {
  const [fontsLoaded] = useFonts({
    RedRose_400Regular,
    RedRose_500Medium,
    RedRose_600SemiBold,
    RedRose_700Bold,
  });

  const [showAssistOverlay, setShowAssistOverlay] = useState(false);
  const [isAssistMode, setIsAssistMode] = useState(false);
  const navigationRef = useRef<any>(null);

  useEffect(() => {
    const handleDeepLink = async (url: string | null) => {
      if (url) {
        const { data, error } = await supabase.auth.exchangeCodeForSession(url);
        if (error) console.error("OAuth callback error:", error);
      }
    };

    Linking.getInitialURL().then(handleDeepLink);
    const sub = Linking.addEventListener("url", ({ url }) => handleDeepLink(url));
    return () => sub.remove();
  }, []);

  useEffect(() => {
    if (fontsLoaded) SplashScreen.hideAsync();
  }, [fontsLoaded]);

  useEffect(() => {
    const handleInitialURL = async () => {
      const url = await Linking.getInitialURL();
      if (url && url.includes("showAssistOverlay=true")) {
        setShowAssistOverlay(true);
        setIsAssistMode(true);
      }
    };

    handleInitialURL();

    const subscription = Linking.addEventListener("url", (event) => {
      if (event.url.includes("showAssistOverlay=true")) {
        setShowAssistOverlay(true);
        setIsAssistMode(true);
      }
    });

    const assistSubscription = DeviceEventEmitter.addListener('onAssistRequested', () => {
      setShowAssistOverlay(true);
      setIsAssistMode(true);
    });

    return () => {
      subscription.remove();
      assistSubscription.remove();
    };
  }, []);

  const handleCloseOverlay = () => {
    setShowAssistOverlay(false);
    setIsAssistMode(false);
    if (isAssistMode) {
      AssistantModule.finishActivity();
    }
  };

  const handleNavigateToChat = () => {
    setShowAssistOverlay(false);
    setIsAssistMode(false);
    if (navigationRef.current) {
      navigationRef.current.navigate("ChatScreen");
    }
  };

  if (!fontsLoaded) return null;

  return (
    <>
      <NavigationContainer ref={navigationRef}>
        <AuthProvider>
          <ThemeProvider>
            <SafeAreaProvider>
              <RootNavigator />
            </SafeAreaProvider>
          </ThemeProvider>
        </AuthProvider>
      </NavigationContainer>

      <AssistantOverlay
        visible={showAssistOverlay}
        onClose={handleCloseOverlay}
        onNavigateToChat={handleNavigateToChat}
      />
    </>
  );
}