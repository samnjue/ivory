//@ts-nocheck
import React from 'react';
import { View, StyleSheet, useColorScheme, Image, TouchableOpacity, Text } from 'react-native';
import { COLORS } from '../constants/colors';
import { LinearGradient } from 'expo-linear-gradient';
import { useNavigation } from '@react-navigation/native';

export default function OnboardingScreen() {
    const colorScheme = useColorScheme();
    const isDark = colorScheme === 'dark';
    const colors = isDark ? COLORS.dark : COLORS.light;
    const navigation = useNavigation();

    return (
        <LinearGradient
            colors={[colors.gradientStart, colors.gradientEnd]}
            style={styles.container}
        >
            <View style={styles.content}>
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
                
                <View style={styles.buttonContainer}>
                    <TouchableOpacity
                        style={[styles.button, { backgroundColor: isDark ? 'rgba(139, 92, 139, 0.6)' : 'rgba(139, 92, 139, 0.5)' }]}
                        onPress={() => navigation.navigate('Register' as never)}
                    >
                        <Text style={styles.buttonText}>Get Started</Text>
                    </TouchableOpacity>
                    
                    <TouchableOpacity
                        style={styles.loginButton}
                        onPress={() => navigation.navigate('Login' as never)}
                    >
                        <Text style={styles.loginText}>Login</Text>
                    </TouchableOpacity>
                </View>
            </View>
        </LinearGradient>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
    },
    content: {
        flex: 1,
        justifyContent: 'space-between',
        alignItems: 'center',
        paddingBottom: 100,
    },
    imageContainer: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
        marginTop: 100,
    },
    starImage: {
        width: 200,
        height: 200,
        marginBottom: 20,
    },
    textImage: {
        width: 250,
        height: 80,
        left: 10
    },
    buttonContainer: {
        width: '100%',
        alignItems: 'center',
        paddingHorizontal: 40,
    },
    button: {
        paddingVertical: 16,
        paddingHorizontal: 20,
        borderRadius: 15,
        width: '85%',
        alignItems: 'center',
        marginBottom: 20,
    },
    buttonText: {
        color: '#fff',
        fontSize: 18,
        fontFamily: "RedRose_400Regular",
        fontWeight: '600',
    },
    loginButton: {
        paddingVertical: 16,
        paddingHorizontal: 20,
        width: '85%',
        alignItems: 'center',
    },
    loginText: {
        color: '#fff',
        fontSize: 18,
        fontFamily: "RedRose_400Regular",
    },
});