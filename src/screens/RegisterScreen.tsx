import React from 'react';  
import { View, Text, StyleSheet } from 'react-native';
import { useTheme } from '../contexts/ThemeContext';
import { COLORS } from '../constants/colors';

export default function RegisterScreen() {
    const { theme } = useTheme();
    const colors = COLORS[theme];

    return (
        <View style={styles.container}>
            <Text style={styles.title}>Register Screen</Text>
            <Text style={styles.text}>{theme}</Text>
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        alignItems: 'center',
        justifyContent: 'center',
    },
    title: {
        fontSize: 20,
        fontWeight: 'bold',
        color: COLORS.dark.text,
    },
    text: {
        fontSize: 14,
        color: COLORS.dark.text,
    },
});