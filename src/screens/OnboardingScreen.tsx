import React from 'react';
import { View, Text, Button, StyleSheet } from 'react-native';
import { useTheme } from '../contexts/ThemeContext';
import { COLORS } from '../constants/colors';
import { LinearGradient } from 'expo-linear-gradient';
import { useNavigation } from '@react-navigation/native';

export default function OnboardingScreen() {
    const { theme } = useTheme();
    const colors = COLORS[theme];
    const navigation = useNavigation();

    return (
        <LinearGradient
            colors={[colors.gradientStart, colors.gradientEnd]}
            style={styles.container}
        >
            <Text
                style={[
                    styles.logo,
                    { color: colors.text, fontFamily: "RedRose_400Regular" }
                ]}
            >ivory</Text>
            <Button title='Get Started' color={colors.primary}/>
            <Button title='Login' color={colors.secondary}/>
        </LinearGradient>
    );
}

const styles = StyleSheet.create({
        container: {
            flex: 1,            
        },
        logo: {
            fontSize: 48,
            marginBottom: 20
        }
});