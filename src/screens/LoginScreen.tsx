import React, { useState } from 'react';
import { View, Text, TextInput, StyleSheet, ActivityIndicator, TouchableOpacity, Alert, useColorScheme, Image } from 'react-native';
import { COLORS } from '../constants/colors';
import { useAuth } from '../contexts/AuthContext';
import { useNavigation } from '@react-navigation/native';
import { Ionicons } from '@expo/vector-icons';

export default function LoginScreen() {
    const colorScheme = useColorScheme();
    const isDark = colorScheme === 'dark';
    const colors = isDark ? COLORS.dark : COLORS.light;
    const { signIn, signInWithGoogle, resetPassword } = useAuth();
    const navigation = useNavigation();
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [loading, setLoading] = useState(false);

    const handleLogin = async () => {
        setLoading(true);
        try {
            await signIn(email, password);
        } catch {
            // Error handled in context
        } finally {
            setLoading(false);
        }
    };

    const handleForgot = async () => {
        if (!email) return Alert.alert('Error', 'Enter your email first');
        try {
            await resetPassword(email);
        } catch {}
    };

    return (
        <View style={[styles.container, { backgroundColor: colors.background }]}>
            {/* Header */}
            <View style={styles.header}>
                <TouchableOpacity 
                    style={[styles.backButton, { backgroundColor: isDark ? '#2a2a2a' : '#f0f0f0' }]}
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
            <Text style={[styles.title, { color: colors.text }]}>Log In to your{'\n'}Account</Text>

            {/* Input Fields */}
            <View style={styles.inputContainer}>
                <View style={[styles.inputWrapper, { backgroundColor: isDark ? '#2a2a2a' : '#f5f5f5' }]}>
                    <Ionicons name="mail-outline" size={20} color={colors.accent} style={styles.icon} />
                    <TextInput
                        style={[styles.input, { color: colors.text }]}
                        placeholder="Enter Your Email"
                        value={email}
                        onChangeText={setEmail}
                        keyboardType="email-address"
                        autoCapitalize="none"
                        placeholderTextColor={colors.accent}
                    />
                </View>

                <View style={[styles.inputWrapper, { backgroundColor: isDark ? '#2a2a2a' : '#f5f5f5' }]}>
                    <Ionicons name="lock-closed-outline" size={20} color={colors.accent} style={styles.icon} />
                    <TextInput
                        style={[styles.input, { color: colors.text }]}
                        placeholder="Password"
                        value={password}
                        onChangeText={setPassword}
                        secureTextEntry={!showPassword}
                        placeholderTextColor={colors.accent}
                    />
                    <TouchableOpacity onPress={() => setShowPassword(!showPassword)}>
                        <Ionicons 
                            name={showPassword ? "eye-outline" : "eye-off-outline"} 
                            size={20} 
                            color={colors.accent} 
                        />
                    </TouchableOpacity>
                </View>
            </View>

            {/* Forgot Password */}
            <TouchableOpacity onPress={handleForgot} style={styles.forgotContainer}>
                <Text style={[styles.forgotText, { color: colors.accent }]}>
                    Forgot Password?
                </Text>
            </TouchableOpacity>

            {/* Login Button */}
            <TouchableOpacity 
                style={[styles.loginButton, { backgroundColor: isDark ? '#e8e8e8' : '#1a1a1a' }]}
                onPress={handleLogin}
                disabled={loading}
            >
                {loading ? (
                    <ActivityIndicator color={isDark ? '#1a1a1a' : '#ffffff'} />
                ) : (
                    <Text style={[styles.loginButtonText, { color: isDark ? '#1a1a1a' : '#ffffff' }]}>
                        Log In
                    </Text>
                )}
            </TouchableOpacity>

            {/* Sign Up Link */}
            <View style={styles.signUpContainer}>
                <Text style={[styles.signUpText, { color: colors.accent }]}>
                    Don't Have An Account?{' '}
                </Text>
                <TouchableOpacity onPress={() => navigation.navigate('Register' as never)}>
                    <Text style={[styles.signUpLink, { color: colors.text, fontWeight: '600' }]}>
                        Sign Up
                    </Text>
                </TouchableOpacity>
            </View>

            {/* Divider */}
            <Text style={[styles.divider, { color: colors.accent }]}>or log in using</Text>

            {/* Google Sign In Button */}
            <TouchableOpacity 
                style={[styles.googleButton, { backgroundColor: isDark ? '#2a2a2a' : '#e8e8e8' }]}
                onPress={signInWithGoogle}
            >
                <Image 
                    source={{ uri: 'https://www.gstatic.com/firebasejs/ui/2.0.0/images/auth/google.svg' }}
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
        width: 40,
        height: 40,
        borderRadius: 20,
        justifyContent: 'center',
        alignItems: 'center',
    },
    logo: {
        height: 30,
        width: 100,
    },
    title: { 
        fontSize: 32, 
        marginBottom: 30,
        fontFamily: 'RedRose_400Regular',
        fontWeight: 'bold',
    },
    inputContainer: {
        marginBottom: 10,
    },
    inputWrapper: {
        flexDirection: 'row',
        alignItems: 'center',
        borderRadius: 12,
        paddingHorizontal: 15,
        marginBottom: 15,
        height: 55,
    },
    icon: {
        marginRight: 10,
    },
    input: { 
        flex: 1,
        fontSize: 15,
    },
    forgotContainer: {
        alignItems: 'flex-end',
        marginBottom: 20,
    },
    forgotText: {
        fontSize: 13,
    },
    loginButton: {
        height: 55,
        borderRadius: 12,
        justifyContent: 'center',
        alignItems: 'center',
        marginBottom: 20,
    },
    loginButtonText: {
        fontSize: 16,
        fontWeight: '600',
    },
    signUpContainer: {
        flexDirection: 'row',
        justifyContent: 'center',
        marginBottom: 20,
    },
    signUpText: {
        fontSize: 14,
    },
    signUpLink: {
        fontSize: 14,
    },
    divider: {
        textAlign: 'center',
        marginVertical: 20,
        fontSize: 13,
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
        fontWeight: '500',
    },
});