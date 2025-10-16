import React from "react";
import { createDrawerNavigator } from "@react-navigation/drawer";
import CustomDrawerContent from "./CustomDrawerContent";
import HomeScreen from "../screens/HomeScreen";
import ChatScreen from "../screens/ChatScreen";
import SettingsScreen from "../screens/SettingsScreen";

const Drawer = createDrawerNavigator();

export default function MainNavigator() {
	return (
		<Drawer.Navigator
			drawerContent={(props) => <CustomDrawerContent {...props} />}
			screenOptions={{
				drawerType: "slide",
				swipeEnabled: true,
				drawerStyle: {
					width: "80%",
					backgroundColor: "transparent",
					shadowColor: "#000",
					shadowOpacity: 0.3,
					shadowRadius: 10,
					elevation: 10,
				},
				overlayColor: "rgba(0,0,0,0.5)",
				headerShown: false,
				swipeEdgeWidth: 250,
				swipeMinDistance: 5,
			}}
		>
			<Drawer.Screen name="Home" component={HomeScreen} />
			<Drawer.Screen name="Chat" component={ChatScreen} />
			<Drawer.Screen name="Settings" component={SettingsScreen} />
		</Drawer.Navigator>
	);
}
