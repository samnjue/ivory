import React, { useState } from 'react';
import { View, Text, TextInput, StyleSheet, ActivityIndicator, TouchableOpacity, useColorScheme, Image, Platform } from 'react-native';
import { COLORS } from '../constants/colors';
import { useAuth } from '../contexts/AuthContext';
import { useNavigation } from '@react-navigation/native';
import { Ionicons } from '@expo/vector-icons';

export default function RegisterScreen() {
    const colorScheme = useColorScheme();
    const isDark = colorScheme === 'dark';
    const colors = isDark ? COLORS.dark : COLORS.light;
    const { signUp, signInWithGoogle } = useAuth();
    const navigation = useNavigation();
    const [fullName, setFullName] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [loading, setLoading] = useState(false);

    const handleRegister = async () => {
        setLoading(true);
        try {
            await signUp(email, password, fullName);
            navigation.navigate("Login" as never);
        } catch {
            // Error handled in context
        } finally {
            setLoading(false);
        }
    };

    return (
        <View style={[styles.container, { backgroundColor: colors.background }]}>
            {/* Header */}
            <View style={styles.header}>
                <TouchableOpacity 
                    style={[styles.backButton, { backgroundColor: isDark ? '#2a2a2a' : '#FFFDFD', ...Platform.select({
                        ios: {},
                        android: {
                            elevation: 15,
                            shadowColor: isDark ? "#FFFFFF" : "#000000ff"
                        }
                    }) }]}
                    onPress={() => navigation.goBack()}
                >
                    <Ionicons name="chevron-back" size={24} color={colors.text} />
                </TouchableOpacity>
                <Image 
                    source={require('../../assets/ivory.png')}
                    style={styles.logo}
                    resizeMode="contain"
                />
            </View>

            {/* Title */}
            <Text style={[styles.title, { color: colors.text }]}>Create your{'\n'}Account</Text>

            {/* Input Fields */}
            <View style={styles.inputContainer}>
                <View style={[styles.inputWrapper, { backgroundColor: isDark ? '#1b1c1dff' : '#FFFDFD' }]}>
                    <Ionicons name="person-outline" size={20} color={colors.text} style={styles.icon} />
                    <TextInput
                        style={[styles.input, { color: colors.text }]}
                        placeholder="Full Name"
                        value={fullName}
                        onChangeText={setFullName}
                        placeholderTextColor={colors.text}
                    />
                </View>

                <View style={[styles.inputWrapper, { backgroundColor: isDark ? '#1b1c1dff' : '#FFFDFD' }]}>
                    <Ionicons name="mail-outline" size={20} color={colors.text} style={styles.icon} />
                    <TextInput
                        style={[styles.input, { color: colors.text }]}
                        placeholder="Enter Your Email"
                        value={email}
                        onChangeText={setEmail}
                        keyboardType="email-address"
                        autoCapitalize="none"
                        placeholderTextColor={colors.text}
                    />
                </View>

                <View style={[styles.inputWrapper, { backgroundColor: isDark ? '#1b1c1dff' : '#FFFDFD' }]}>
                    <Ionicons name="lock-closed-outline" size={20} color={colors.text} style={styles.icon} />
                    <TextInput
                        style={[styles.input, { color: colors.text }]}
                        placeholder="Password"
                        value={password}
                        onChangeText={setPassword}
                        secureTextEntry={!showPassword}
                        placeholderTextColor={colors.text}
                        autoCapitalize='none'
                    />
                    <TouchableOpacity onPress={() => setShowPassword(!showPassword)}>
                        <Ionicons 
                            name={showPassword ? "eye-outline" : "eye-off-outline"} 
                            size={20} 
                            color={colors.text} 
                        />
                    </TouchableOpacity>
                </View>
            </View>

            {/* Register Button */}
            <TouchableOpacity 
                style={[styles.registerButton, { backgroundColor: isDark ? '#e8e8e8' : '#1a1a1a' }]}
                onPress={handleRegister}
                disabled={loading}
            >
                {loading ? (
                    <ActivityIndicator color={isDark ? '#1a1a1a' : '#ffffff'} />
                ) : (
                    <Text style={[styles.registerButtonText, { color: isDark ? '#1a1a1a' : '#ffffff' }]}>
                        Register
                    </Text>
                )}
            </TouchableOpacity>

            {/* Sign In Link */}
            <View style={styles.signInContainer}>
                <Text style={[styles.signInText, { color: "#818181" }]}>
                    Already Have An Account?{' '}
                </Text>
                <TouchableOpacity onPress={() => navigation.navigate("Login" as never)}>
                    <Text style={[styles.signInLink, { color: colors.text, fontWeight: '600' }]}>
                        Sign In
                    </Text>
                </TouchableOpacity>
            </View>

            {/* Divider */}
            <Text style={[styles.divider, { color: "#818181" }]}>or sign up using</Text>

            {/* Google Sign In Button */}
            <TouchableOpacity 
                style={[styles.googleButton, { backgroundColor: isDark ? '#2a2a2a' : '#fffffd' }]}
                onPress={signInWithGoogle}
            >
                <Image 
                    source={require("../../assets/icons/Google.png")}
                    style={styles.googleIcon}
                />
                <Text style={[styles.googleButtonText, { color: colors.text }]}>
                    Continue with Google
                </Text>
            </TouchableOpacity>
        </View>
    );
};

const styles = StyleSheet.create({
    container: { 
        flex: 1, 
        padding: 20,
    },
    header: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        marginTop: 40,
        marginBottom: 40,
        position: 'relative',
    },
    backButton: {
        position: 'absolute',
        left: 0,
        width: 43,
        height: 43,
        borderRadius: 13,
        justifyContent: 'center',
        alignItems: 'center',
    },
    logo: {
        height: 30,
        width: 100,
    },
    title: { 
        fontSize: 36, 
        marginBottom: 30,
        fontFamily: 'RedRose_700Bold',
        fontWeight: '600',
    },
    inputContainer: {
        marginBottom: 20,
    },
    inputWrapper: {
        flexDirection: 'row',
        alignItems: 'center',
        borderRadius: 12,
        paddingHorizontal: 15,
        marginBottom: 15,
        height: 65,
    },
    icon: {
        marginRight: 10,
    },
    input: { 
        flex: 1,
        fontSize: 15,
        fontFamily: "RedRose_400Regular",
        top: 1
    },
    registerButton: {
        height: 65,
        borderRadius: 12,
        justifyContent: 'center',
        alignItems: 'center',
        marginBottom: 20,
    },
    registerButtonText: {
        fontSize: 16,
        fontFamily: "RedRose_600SemiBold",
        fontWeight: '600',
    },
    signInContainer: {
        flexDirection: 'row',
        justifyContent: 'center',
        marginBottom: 20,
    },
    signInText: {
        fontSize: 14,
        fontFamily: "RedRose_400Regular",
    },
    signInLink: {
        fontSize: 14,
        fontFamily: "RedRose_500Medium",
    },
    divider: {
        textAlign: 'center',
        marginVertical: 20,
        fontSize: 13,
        fontFamily: "RedRose_400Regular",
    },
    googleButton: {
        height: 55,
        borderRadius: 12,
        flexDirection: 'row',
        justifyContent: 'center',
        alignItems: 'center',
    },
    googleIcon: {
        width: 20,
        height: 20,
        marginRight: 10,
    },
    googleButtonText: {
        fontSize: 15,
        fontFamily: "RedRose_500Medium",
        fontWeight: '500',
        top: 3
    },
});