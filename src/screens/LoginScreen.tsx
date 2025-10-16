import React, { useState } from 'react';
import { View, Text, TextInput, Button, StyleSheet, ActivityIndicator, TouchableOpacity, Alert, useColorScheme } from 'react-native';
import { COLORS } from '../constants/colors';
import { useAuth } from '../contexts/AuthContext';
import { useNavigation } from '@react-navigation/native';

const LoginScreen = () => {
    const colorScheme = useColorScheme();
    const isDark = colorScheme === 'dark';
    const colors = isDark ? COLORS.dark : COLORS.light;
    const { signIn, signInWithGoogle, resetPassword } = useAuth();
    const navigation = useNavigation();
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
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
        <Text style={[styles.title, { color: colors.text }]}>Log In to Your Account</Text>
        <TextInput
            style={[styles.input, { backgroundColor: colors.secondary, color: colors.text }]}
            placeholder="Enter Your Email"
            value={email}
            onChangeText={setEmail}
            keyboardType="email-address"
            autoCapitalize="none"
            placeholderTextColor={colors.accent}
        />
        <TextInput
            style={[styles.input, { backgroundColor: colors.secondary, color: colors.text }]}
            placeholder="Password"
            value={password}
            onChangeText={setPassword}
            secureTextEntry
            placeholderTextColor={colors.accent}
        />
        <TouchableOpacity onPress={handleForgot}>
            <Text style={[styles.link, { color: colors.accent }]}>Forgot Password?</Text>
        </TouchableOpacity>
        {loading ? (
            <ActivityIndicator color={colors.primary} />
        ) : (
            <Button title="Log In" onPress={handleLogin} color={colors.primary} />
        )}
        <TouchableOpacity onPress={() => navigation.navigate('Register' as never)}>
            <Text style={[styles.link, { color: colors.accent }]}>Don't Have An Account? Sign Up</Text>
        </TouchableOpacity>
        <Button title="Continue with Google" onPress={signInWithGoogle} color={colors.secondary} />
        </View>
    );
};

const styles = StyleSheet.create({
  container: { 
    flex: 1, 
    justifyContent: 'center', 
    padding: 20 
  },
  title: { 
    fontSize: 24, 
    marginBottom: 20, 
    textAlign: 'center', 
    fontFamily: 'RedRose_400Regular' 
  },
  input: { 
    padding: 10, 
    marginBottom: 10, 
    borderRadius: 5 
  },
  link: { 
    textAlign: 'right', 
    marginBottom: 10 
  },
});

export default LoginScreen;