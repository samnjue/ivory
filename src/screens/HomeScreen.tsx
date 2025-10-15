import React from 'react';
import { View, Text, Button, StyleSheet } from 'react-native';
import { useTheme } from '../contexts/ThemeContext';
import { COLORS } from '../constants/colors';
import { useNavigation, DrawerActions } from '@react-navigation/native';

export default function HomeScreen() {
    const { theme } = useTheme();
    const colors = COLORS[theme];
    const navigation = useNavigation();

    return (
        <View style={styles.container}>
            <Text style={styles.title}>Home Screen</Text>
            <Text style={styles.text}>{theme}</Text>
            <Button
                title="Open Menu"
                color={colors.primary}
                onPress={() => navigation.dispatch(DrawerActions.openDrawer())}
            />
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