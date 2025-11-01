import React, { useState, useEffect } from "react";
import {
  View,
  TextInput,
  TouchableOpacity,
  Image,
  StyleSheet,
  Keyboard,
  Dimensions,
} from "react-native";
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withTiming,
  withSpring,
} from "react-native-reanimated";
import { Paperclip, SendHorizontal } from "lucide-react-native";
import { LinearGradient } from "expo-linear-gradient";
import { COLORS } from "../constants/colors";
import { useColorScheme } from "react-native";
import { NativeModules } from "react-native";

const { width } = Dimensions.get("window");
const { Assistant } = NativeModules;

export default function OverlayInputBar() {
  const isDark = useColorScheme() === "dark";
  const colors = isDark ? COLORS.dark : COLORS.light;

  const [text, setText] = useState("");

  const translateY = useSharedValue(0);
  const starOpacity = useSharedValue(1);
  const sendOpacity = useSharedValue(0);
  const sendScale = useSharedValue(0);

  // Keyboard lift
  useEffect(() => {
    const show = Keyboard.addListener("keyboardDidShow", () => {
      translateY.value = withSpring(-25);
    });
    const hide = Keyboard.addListener("keyboardDidHide", () => {
      translateY.value = withSpring(0);
    });
    return () => { show.remove(); hide.remove(); };
  }, []);

  // Swap ivory-star to Send
  useEffect(() => {
    if (text) {
      starOpacity.value = withTiming(0, { duration: 150 });
      sendOpacity.value = withTiming(1, { duration: 150 });
      sendScale.value = withSpring(1);
    } else {
      starOpacity.value = withTiming(1, { duration: 150 });
      sendOpacity.value = withTiming(0, { duration: 150 });
      sendScale.value = withSpring(0);
    }
  }, [text]);

  const barStyle = useAnimatedStyle(() => ({
    transform: [{ translateY: translateY.value }],
  }));

  const starStyle = useAnimatedStyle(() => ({ opacity: starOpacity.value }));
  const sendStyle = useAnimatedStyle(() => ({
    opacity: sendOpacity.value,
    transform: [{ scale: sendScale.value }],
  }));

  const handleSend = () => {
    if (text.trim()) {
      Assistant.sendText(text.trim());
      setText("");
    }
  };

  const handleIvoryStar = () => {
    Assistant.openMainApp();
  };

  return (
    <Animated.View style={[styles.bar, barStyle]}>
      <TouchableOpacity style={styles.clipBtn}>
        <Paperclip size={24} color={colors.text} />
      </TouchableOpacity>

      <TextInput
        style={[styles.input, { color: colors.text }]}
        placeholder="Ask anything"
        placeholderTextColor={colors.text + "88"}
        value={text}
        onChangeText={setText}
        multiline
      />

      {/* Ivory-star (visible when no text) */}
      <Animated.View style={[styles.rightBtn, starStyle]}>
        <TouchableOpacity onPress={handleIvoryStar}>
          <LinearGradient
            colors={["#e63946", "#4285f4"]}
            start={{ x: 0, y: 0 }}
            end={{ x: 1, y: 1 }}
            style={styles.gradientBorder}
          >
            <View style={styles.innerLogo}>
              <Image source={require("../../assets/ivorystar.png")} style={styles.logo} />
            </View>
          </LinearGradient>
        </TouchableOpacity>
      </Animated.View>

      {/* Send (visible when text) */}
      <Animated.View style={[styles.rightBtn, sendStyle]}>
        <TouchableOpacity onPress={handleSend}>
          <SendHorizontal size={26} color={colors.text} />
        </TouchableOpacity>
      </Animated.View>
    </Animated.View>
  );
}

const styles = StyleSheet.create({
  bar: {
    position: "absolute",
    left: 16,
    right: 16,
    bottom: 20,
    flexDirection: "row",
    alignItems: "flex-end",
    backgroundColor: "#151515",
    borderRadius: 30,
    padding: 8,
    elevation: 20,
    shadowColor: "#000",
    shadowOpacity: 0.3,
    shadowRadius: 12,
    shadowOffset: { width: 0, height: 8 },
  },
  clipBtn: { width: 40, height: 40, justifyContent: "center", alignItems: "center" },
  input: { flex: 1, fontSize: 16, paddingHorizontal: 12, maxHeight: 120 },
  rightBtn: { position: "absolute", right: 8, bottom: 8 },
  gradientBorder: { width: 43, height: 43, borderRadius: 22, justifyContent: "center", alignItems: "center" },
  innerLogo: { width: 38, height: 38, borderRadius: 19, backgroundColor: "#151515", justifyContent: "center", alignItems: "center" },
  logo: { width: 24, height: 24 },
});