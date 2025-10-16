import React from 'react';
import { View, Text, StyleSheet, Image, TouchableOpacity, useColorScheme } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { COLORS } from '../constants/colors';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

export default function HomeScreen({ navigation }: any) {
  const colorScheme = useColorScheme();
  const isDark = colorScheme === 'dark';
  const colors = isDark ? COLORS.dark : COLORS.light;
  const insets = useSafeAreaInsets();

  const logoSource = require('../../assets/ivorystar.png');
  const avatarSource = require('../../assets/20250805_160609.jpg');

  return (
    <View style={[styles.container, { backgroundColor: colors.background }]}>
      {/* Profile Avatar - Top Left */}
      <TouchableOpacity 
        style={[styles.avatarContainer, { top: insets.top + 10 }]}
        onPress={() => navigation.openDrawer()}
      >
        <Image source={avatarSource} style={styles.avatar} />
      </TouchableOpacity>

      {/* Brand Image - Top Center */}
      <Image 
        source={require("../../assets/ivory.png")} 
        style={[styles.brand, { top: insets.top + 15 }]} 
        resizeMode="contain"
      />

      {/* Center Content */}
      <View style={styles.centerContent}>
        {/* Logo with gradient border and shadow - Now a TouchableOpacity */}
        <TouchableOpacity 
          style={styles.shadowContainer}
          onPress={() => navigation.openDrawer()}
          activeOpacity={0.8}
        >
          <LinearGradient
            colors={['#e63946', '#4285f4']}
            start={{ x: 0, y: 0 }}
            end={{ x: 1, y: 1 }}
            style={styles.gradientBorder}
          >
            <View style={[styles.logoInnerContainer, { backgroundColor: colors.background }]}>
              <Image source={logoSource} style={styles.logo} />
            </View>
          </LinearGradient>
        </TouchableOpacity>
        
        <TouchableOpacity 
          onPress={() => navigation.openDrawer()}
          activeOpacity={0.7}
        >
          <Text style={[styles.startText, { color: colors.text }]}>
            Tap to start ivory
          </Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  avatarContainer: {
    position: 'absolute',
    left: 16,
    zIndex: 10,
  },
  avatar: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: '#ddd',
  },
  brand: {
    position: 'absolute',
    alignSelf: 'center',
    height: 35,
    width: 120,
    bottom: 10
  },
  centerContent: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  shadowContainer: {
    marginBottom: 48,
    shadowColor: '#4285f4',
    shadowOffset: {
      width: 5,
      height: 20,
    },
    shadowOpacity: 1,
    shadowRadius: 16,
    elevation: 40,
    borderRadius: 140,
  },
  gradientBorder: {
    width: 245,
    height: 245,
    borderRadius: 190,
    justifyContent: 'center',
    alignItems: 'center',
  },
  logoInnerContainer: {
    width: 228,
    height: 228,
    borderRadius: 134,
    justifyContent: 'center',
    alignItems: 'center',
  },
  logo: {
    width: 150,
    height: 150,
  },
  startText: {
    fontSize: 25,
    fontFamily: 'RedRose_600SemiBold',
  },
});