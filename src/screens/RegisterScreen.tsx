import React, { useState } from 'react';
import { View, Text, TextInput, Button, StyleSheet, ActivityIndicator, TouchableOpacity, useColorScheme } from 'react-native';
import { COLORS } from '../constants/colors';
import { useAuth } from '../contexts/AuthContext';
import { useNavigation } from '@react-navigation/native';

const RegisterScreen = () => {
    const colorScheme = useColorScheme();
    const isDark = colorScheme === 'dark';
    const colors = isDark ? COLORS.dark : COLORS.light;
    const { signUp, signInWithGoogle } = useAuth();
    const navigation = useNavigation();
    const [fullName, setFullName] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
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
        <Text style={[styles.title, { color: colors.text }]}>Create Your Account</Text>
        <TextInput
            style={[styles.input, { backgroundColor: colors.secondary, color: colors.text }]}
            placeholder="Full Name"
            value={fullName}
            onChangeText={setFullName}
            placeholderTextColor={colors.accent}
        />
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
        {loading ? (
            <ActivityIndicator color={colors.primary} />
        ) : (
            <Button title="Register" onPress={handleRegister} color={colors.primary} />
        )}
        <TouchableOpacity onPress={() => navigation.navigate("Login" as never)}>
            <Text style={[styles.link, { color: colors.accent }]}>Already Have An Account? Sign In</Text>
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
    textAlign: 'center', 
    marginVertical: 10 
  },
});

export default RegisterScreen;