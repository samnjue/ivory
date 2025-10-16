import React from "react";
import { createStackNavigator } from "@react-navigation/stack";
import OnboardingScreen from "../screens/OnboardingScreen";
import RegisterScreen from "../screens/RegisterScreen";
import LoginScreen from "../screens/LoginScreen";
import {
	CardStyleInterpolators,
	TransitionSpecs,
} from "@react-navigation/stack";

const Stack = createStackNavigator();

export default function AuthNavigator() {
	return (
		<Stack.Navigator screenOptions={{ headerShown: false }}>
			<Stack.Screen name="Onboarding" component={OnboardingScreen} />
			<Stack.Screen
				name="Register"
				component={RegisterScreen}
				options={{
					cardStyleInterpolator: CardStyleInterpolators.forHorizontalIOS,
					transitionSpec: {
						open: TransitionSpecs.TransitionIOSSpec,
						close: TransitionSpecs.TransitionIOSSpec,
					},
				}}
			/>
			<Stack.Screen
				name="Login"
				component={LoginScreen}
				options={{
					cardStyleInterpolator: CardStyleInterpolators.forHorizontalIOS,
					transitionSpec: {
						open: TransitionSpecs.TransitionIOSSpec,
						close: TransitionSpecs.TransitionIOSSpec,
					},
				}}
			/>
		</Stack.Navigator>
	);
}
