//@ts-nocheck
import React from 'react';
import { View, StyleSheet, useColorScheme, Image, TouchableOpacity, Text } from 'react-native';
import { useTheme } from '../contexts/ThemeContext';
import { COLORS } from '../constants/colors';
import { LinearGradient } from 'expo-linear-gradient';
import { useNavigation } from '@react-navigation/native';

export default function OnboardingScreen() {
    const colorScheme = useColorScheme();
    const isDark = colorScheme === 'dark';
    const colors = isDark === 'dark' ? COLORS.dark : COLORS.light;
    const navigation = useNavigation();

    return (
        <LinearGradient
            colors={[colors.gradientStart, colors.gradientEnd]}
            style={styles.container}
        >
            <View style={styles.imageContainer}>
                <Image
                    source={require('../../assets/ivorystar.png')}
                    style={styles.starImage}
                    resizeMode="contain"
                />
                <Image
                    source={require('../../assets/ivory.png')}
                    style={styles.textImage}
                    resizeMode="contain"
                />
            </View>
            <TouchableOpacity
                style={[styles.button, { backgroundColor: colors.primary }]}
                onPress={() => {/* Handle Get Started */}}
            >
                <Text style={styles.buttonText}>Get Started</Text>
            </TouchableOpacity>
            <TouchableOpacity
                style={[styles.button, { backgroundColor: colors.secondary }]}
                onPress={() => {/* Handle Login */}}
            >
                <Text style={styles.buttonText}>Login</Text>
            </TouchableOpacity>
        </LinearGradient>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
    },
    imageContainer: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
    },
    starImage: {
        width: 200,
        height: 200,
    },
    textImage: {
        width: 150,
        height: 50,
        //marginTop: -20,
    },
    button: {
        paddingVertical: 10,
        paddingHorizontal: 20,
        borderRadius: 10,
        marginVertical: 10,
        width: '80%',
        alignItems: 'center',
        bottom: 120
    },
    buttonText: {
        color: '#fff',
        fontSize: 16,
        fontFamily: "RedRose_400Regular",
    },
});