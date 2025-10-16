import "react-native-gesture-handler";
import React, { useState, useEffect } from "react";
import { View } from "react-native";
import { GestureHandlerRootView } from "react-native-gesture-handler";
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

	useEffect(() => {
		const handleDeepLink = async (url: string | null) => {
			if (url) {
				const { data, error } = await supabase.auth.exchangeCodeForSession(url);
				if (error) console.error("OAuth callback error:", error);
			}
		};

		Linking.getInitialURL().then(handleDeepLink);
		const sub = Linking.addEventListener("url", ({ url }) =>
			handleDeepLink(url)
		);
		return () => sub.remove();
	}, []);

	useEffect(() => {
		if (fontsLoaded) {
			SplashScreen.hideAsync();
		}
	}, [fontsLoaded]);

	if (!fontsLoaded) return null;

	return (
		<AuthProvider>
			<ThemeProvider>
				<SafeAreaProvider>
					<NavigationContainer>
						<RootNavigator />
					</NavigationContainer>
				</SafeAreaProvider>
			</ThemeProvider>
		</AuthProvider>
	);
}
