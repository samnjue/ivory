import React from 'react';
import { createDrawerNavigator } from '@react-navigation/drawer';
import HomeScreen from '../screens/HomeScreen';
import ChatScreen from '../screens/ChatScreen';
import SettingsScreen from '../screens/SettingsScreen';

const Drawer = createDrawerNavigator();

export default function MainNavigator() {
    return (
        <Drawer.Navigator
            screenOptions={{
                drawerType: "slide",
                drawerStyle: {
                    width: '80%',
                    backgroundColor: 'transparent', 
                    shadowColor: '#000',
                    shadowOpacity: 0.3,
                    shadowRadius: 10,
                    elevation: 10,
                },
                overlayColor: "rgba(0,0,0,0.5)",
                headerShown: false,
            }}
        >
            <Drawer.Screen name="Home" component={HomeScreen} />
            <Drawer.Screen name="Chat" component={ChatScreen} />
            <Drawer.Screen name="Settings" component={SettingsScreen} />
        </Drawer.Navigator>
    )
}