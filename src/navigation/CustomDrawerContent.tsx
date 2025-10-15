import React from 'react';
import { View, Text, StyleSheet, Image, TouchableOpacity } from 'react-native';
import { DrawerContentScrollView, DrawerItemList, DrawerItem } from '@react-navigation/drawer';
import { useTheme } from '../contexts/ThemeContext';
import { COLORS } from '../constants/colors';

export default function CustomDrawerContent(props: any) {
    const { theme } = useTheme();
    const colors = COLORS[theme];

    return (
        <DrawerContentScrollView
            {...props}
            contentContainerStyle={[styles.container, { backgroundColor: colors.background }]}
        >
            <View style={styles.header}>
                <TouchableOpacity onPress={() => props.navigation.closeDrawer()}>
                    <Image
                        style={styles.avatar}
                    />
                </TouchableOpacity>
                <View style={styles.userInfo}>
                    <Text style={[styles.name, { color: colors.text, fontFamily: 'RedRose_600SemiBold' }]}>
                        Hello
                    </Text>
                    <Text style={[styles.email, { color: colors.primary }]}>you@example.com</Text>
                </View>
            </View>

            <View style={styles.list}>
                <DrawerItemList {...props} />
            </View>

            <View style={styles.footer}>
                <DrawerItem
                    label="Settings"
                    onPress={() => props.navigation.navigate('Settings')}
                    labelStyle={{ color: colors.text }}
                />
                <DrawerItem
                    label="Sign Out"
                    onPress={() => {/* handle sign out */}}
                    labelStyle={{ color: colors.text }}
                />
            </View>
        </DrawerContentScrollView>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        paddingTop: 0,
    },
    header: {
        flexDirection: 'row',
        alignItems: 'center',
        padding: 20,
        borderBottomWidth: StyleSheet.hairlineWidth,
        borderBottomColor: '#00000022',
    },
    avatar: {
        width: 56,
        height: 56,
        borderRadius: 28,
        backgroundColor: '#ddd',
    },
    userInfo: {
        marginLeft: 12,
    },
    name: {
        fontSize: 18,
    },
    email: {
        fontSize: 12,
        marginTop: 2,
    },
    list: {
        flex: 1,
    },
    footer: {
        borderTopWidth: StyleSheet.hairlineWidth,
        borderTopColor: '#00000022',
    },
});