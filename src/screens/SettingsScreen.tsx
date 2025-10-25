import React, { useState, useEffect } from "react";
import {
  View,
  Text,
  StyleSheet,
  Platform,
  Image,
  useColorScheme,
  Alert,
} from "react-native";
import { TouchableOpacity } from "react-native";
import Ionicons from "@expo/vector-icons/Ionicons";
import { LinearGradient } from "expo-linear-gradient";
import { Edit, UserRoundCog, LogOut, PenLine, ShieldCheck, Mic } from "lucide-react-native";
import SwitchToggle from "react-native-switch-toggle";
import { COLORS } from "../constants/colors";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { supabase } from "../utils/supabase";
import AssistantModule from "../modules/AssistantModule";

export default function SettingsScreen({ navigation }: any) {
  const colorScheme = useColorScheme();
  const isDark = colorScheme === "dark";
  const colors = isDark ? COLORS.dark : COLORS.light;

  const insets = useSafeAreaInsets();

  const [isDefaultAssistant, setIsDefaultAssistant] = useState(false);
  const [hasOverlayPerm, setHasOverlayPerm] = useState(false);
  const [hasMicPerm, setHasMicPerm] = useState(false);

  const backgroundActive = isDark ? "#FFFFFF" : "#000000";
  const backgroundInactive = isDark ? "#6e6e6eff" : "#b6b6b6ff";
  const circleColor = isDark ? "#000000" : "#FFFFFF";

  useEffect(() => {
    checkPermissions();
  }, []);

  useEffect(() => {
    const unsubscribe = navigation.addListener("focus", () => {
      checkPermissions();
    });

    return unsubscribe;
  }, [navigation]);

  const checkPermissions = async () => {
    try {
      const assistEnabled = await AssistantModule.isAssistantEnabled();
      const overlayEnabled = await AssistantModule.hasOverlayPermission();
      const micEnabled = Platform.OS === "android" ? await AssistantModule.requestMicrophonePermission() : true;

      console.log("Assistant enabled:", assistEnabled);
      console.log("Overlay permission:", overlayEnabled);
      console.log("Microphone permission:", micEnabled);

      setIsDefaultAssistant(assistEnabled);
      setHasOverlayPerm(overlayEnabled);
      setHasMicPerm(micEnabled);
    } catch (error) {
      console.error("Error checking permissions:", error);
    }
  };

  const handleToggleAssistant = async () => {
    try {
      const success = await AssistantModule.requestAssistPermission();
      if (success) {
        Alert.alert(
          isDefaultAssistant ? "Change Assistant" : "Enable Ivory Assistant",
          isDefaultAssistant
            ? "You will be redirected to Android settings. Please:\n\n" +
              "1. Tap on 'Assist app'\n" +
              "2. Select another app or none to disable Ivory as default assistant."
            : "You will be redirected to Android settings. Please:\n\n" +
              "1. Tap on 'Assist app'\n" +
              "2. Select 'Ivory' from the list\n" +
              "3. Grant any requested permissions\n\n" +
              "After enabling, swipe up from the home button to activate Ivory!",
          [
            {
              text: "Got it",
              onPress: () => checkPermissions(),
            },
          ]
        );
      } else {
        Alert.alert("Error", "Failed to open assistant settings. Please try again.");
      }
    } catch (error) {
      console.error("Error requesting assist permission:", error);
      Alert.alert("Error", "Failed to open assistant settings. Please try again.");
    }
  };

  const handleToggleOverlayPermission = async () => {
    try {
      if (hasOverlayPerm) {
        Alert.alert(
          "Overlay Permission Enabled",
          "To disable overlay permission, please go to:\n\n" +
          "Settings > Apps > Ivory > Advanced > Display over other apps",
          [{ text: "OK" }]
        );
        return;
      }

      const success = await AssistantModule.requestOverlayPermission();
      if (success) {
        Alert.alert(
          "Enable Overlay Permission",
          "You will be redirected to Android settings. Please:\n\n" +
          "1. Toggle 'Allow display over other apps' ON\n" +
          "2. Come back to Ivory\n\n" +
          "This permission is required for the assistant overlay to appear on top of other apps.",
          [
            {
              text: "Open Settings",
              onPress: () => {
                setTimeout(() => checkPermissions(), 1000);
              },
            },
          ]
        );
      }
    } catch (error) {
      console.error("Error requesting overlay permission:", error);
      Alert.alert("Error", "Failed to open overlay settings. Please try again.");
    }
  };

  const handleToggleMicrophonePermission = async () => {
    try {
      if (hasMicPerm) {
        Alert.alert(
          "Microphone Permission Enabled",
          "To disable microphone permission, please go to:\n\n" +
          "Settings > Apps > Ivory > Permissions > Microphone",
          [{ text: "OK" }]
        );
        return;
      }

      const success = await AssistantModule.requestMicrophonePermission();
      if (success) {
        setHasMicPerm(true);
        Alert.alert(
          "Microphone Permission Granted",
          "Microphone access is now enabled for voice input.",
          [{ text: "OK" }]
        );
      } else {
        Alert.alert(
          "Enable Microphone Permission",
          "You will be redirected to Android settings. Please:\n\n" +
          "1. Toggle 'Microphone' ON\n" +
          "2. Come back to Ivory\n\n" +
          "This permission is required for voice input in the assistant.",
          [
            {
              text: "Open Settings",
              onPress: () => {
                setTimeout(() => checkPermissions(), 1000);
              },
            },
          ]
        );
      }
    } catch (error) {
      console.error("Error requesting microphone permission:", error);
      Alert.alert("Error", "Failed to request microphone permission. Please try again.");
    }
  };

  return (
    <View
      style={[
        styles.container,
        {
          backgroundColor: colors.background,
          paddingTop: insets.top,
          paddingBottom: insets.bottom,
        },
      ]}
    >
      <View style={styles.header}>
        <TouchableOpacity
          style={[
            styles.backButton,
            {
              backgroundColor: isDark ? "#2a2a2a" : "#FFFDFD",
              ...Platform.select({
                ios: {},
                android: {
                  elevation: 15,
                  shadowColor: isDark ? "#FFFFFF" : "#000000ff",
                },
              }),
            },
          ]}
          onPress={() => navigation.goBack()}
        >
          <Ionicons name="chevron-back" size={24} color={colors.text} />
        </TouchableOpacity>
        <Text style={[styles.title, { color: colors.text }]}>Settings</Text>
      </View>
      
      <View style={styles.profileContainer}>
        <View style={styles.profileImageWrapper}>
          <Image
            source={require("../../assets/20250805_160609.jpg")}
            style={styles.profileImage}
          />
          <TouchableOpacity
            style={[
              styles.editButton,
              { backgroundColor: isDark ? "#303030ff" : "#f1ededff" },
            ]}
          >
            <PenLine color={colors.text} size={16} />
          </TouchableOpacity>
        </View>
        <Text style={[styles.userName, { color: colors.text }]}>
          Sammy Njue
        </Text>
        <Text style={[styles.userEmail, { color: colors.text }]}>
          sammynjue10@gmail.com
        </Text>
      </View>

      <View style={styles.preferencesContainer}>
        <Text
          style={[
            styles.sectionTitle,
            { color: isDark ? "#b9b9b9ff" : "#4B4B4B" },
          ]}
        >
          Permissions
        </Text>

        {/* Default Assistant Toggle */}
        <View style={styles.preferenceRow}>
          <View style={[styles.preferenceLeft, { left: 9 }]}>
            <UserRoundCog color={colors.text} size={24} />
            <Text
              style={[styles.preferenceText, { color: colors.text, left: 5 }]}
            >
              Set as default Assistant
            </Text>
          </View>
          <SwitchToggle
            switchOn={isDefaultAssistant}
            onPress={handleToggleAssistant}
            backgroundColorOn={backgroundActive}
            backgroundColorOff={backgroundInactive}
            circleColorOn={circleColor}
            circleColorOff={circleColor}
            containerStyle={styles.switchContainer}
            circleStyle={styles.switchCircle}
          />
        </View>

        {/* Overlay Permission Toggle */}
        <View style={styles.preferenceRow}>
          <View style={[styles.preferenceLeft, { left: 9 }]}>
            <ShieldCheck color={colors.text} size={24} />
            <Text
              style={[styles.preferenceText, { color: colors.text, left: 5 }]}
            >
              Overlay permission
            </Text>
          </View>
          <SwitchToggle
            switchOn={hasOverlayPerm}
            onPress={handleToggleOverlayPermission}
            backgroundColorOn={backgroundActive}
            backgroundColorOff={backgroundInactive}
            circleColorOn={circleColor}
            circleColorOff={circleColor}
            containerStyle={styles.switchContainer}
            circleStyle={styles.switchCircle}
          />
        </View>

        {/* Microphone Permission Toggle */}
        <View style={styles.preferenceRow}>
          <View style={[styles.preferenceLeft, { left: 9 }]}>
            <Mic color={colors.text} size={24} />
            <Text
              style={[styles.preferenceText, { color: colors.text, left: 5 }]}
            >
              Microphone permission
            </Text>
          </View>
          <SwitchToggle
            switchOn={hasMicPerm}
            onPress={handleToggleMicrophonePermission}
            backgroundColorOn={backgroundActive}
            backgroundColorOff={backgroundInactive}
            circleColorOn={circleColor}
            circleColorOff={circleColor}
            containerStyle={styles.switchContainer}
            circleStyle={styles.switchCircle}
          />
        </View>

        <Text
          style={[
            styles.helperText,
            { color: isDark ? "#999999" : "#666666" },
          ]}
        >
          Both permissions are required for the assistant overlay to work properly.
        </Text>
      </View>

      <View style={styles.preferencesContainer}>
        <Text
          style={[
            styles.sectionTitle,
            { color: isDark ? "#b9b9b9ff" : "#4B4B4B" },
          ]}
        >
          Preferences
        </Text>
        
        <View style={styles.preferenceRow}>
          <View style={styles.preferenceLeft}>
            <LinearGradient
              colors={["#e63946", "#4285f4"]}
              start={{ x: 0, y: 0 }}
              end={{ x: 1, y: 1 }}
              style={styles.miniGradientBorder}
            >
              <View
                style={[
                  styles.miniLogoInnerContainer,
                  { backgroundColor: colors.background },
                ]}
              >
                <Image
                  source={require("../../assets/ivorystar.png")}
                  style={styles.miniLogo}
                />
              </View>
            </LinearGradient>
            <Text style={[styles.preferenceText, { color: colors.text }]}>
              Enable floating button
            </Text>
          </View>
          <SwitchToggle
            switchOn={false}
            onPress={() => {
              Alert.alert("Coming Soon", "Floating button feature is coming soon!");
            }}
            backgroundColorOn={backgroundActive}
            backgroundColorOff={backgroundInactive}
            circleColorOn={circleColor}
            circleColorOff={circleColor}
            containerStyle={styles.switchContainer}
            circleStyle={styles.switchCircle}
          />
        </View>
      </View>

      <TouchableOpacity
        style={styles.logoutButton}
        onPress={() => supabase.auth.signOut()}
      >
        <LogOut color="#FF0000" size={26} style={{ left: 8 }} />
        <Text style={styles.logoutText}>Log out</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    paddingHorizontal: 20,
  },
  header: {
    height: 60,
    justifyContent: "center",
    alignItems: "center",
    position: "relative",
  },
  backButton: {
    position: "absolute",
    left: 0,
    width: 40,
    height: 40,
    borderRadius: 13,
    justifyContent: "center",
    alignItems: "center",
  },
  title: {
    fontSize: 20,
    fontFamily: "RedRose_500Medium",
  },
  profileContainer: {
    alignItems: "center",
    marginTop: 20,
  },
  profileImageWrapper: {
    position: "relative",
  },
  profileImage: {
    width: 80,
    height: 80,
    borderRadius: 40,
  },
  editButton: {
    position: "absolute",
    top: 70,
    right: 10,
    width: 28,
    height: 28,
    borderRadius: 14,
    justifyContent: "center",
    alignItems: "center",
    transform: [{ translateX: 14 }, { translateY: -14 }],
  },
  userName: {
    fontSize: 18,
    fontFamily: "RedRose_600SemiBold",
    marginTop: 10,
  },
  userEmail: {
    fontSize: 11,
    fontFamily: "RedRose_400Regular",
    marginTop: 0,
    opacity: 0.7,
  },
  preferencesContainer: {
    marginTop: 30,
    width: "100%",
  },
  sectionTitle: {
    fontSize: 18,
    fontFamily: "RedRose_600SemiBold",
    marginBottom: 10,
  },
  preferenceRow: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    marginVertical: 15,
  },
  preferenceLeft: {
    flexDirection: "row",
    alignItems: "center",
  },
  preferenceText: {
    fontSize: 16,
    fontFamily: "RedRose_500Medium",
    marginLeft: 10,
  },
  helperText: {
    fontSize: 12,
    fontFamily: "RedRose_400Regular",
    marginTop: 5,
    marginLeft: 9,
    lineHeight: 18,
  },
  miniGradientBorder: {
    width: 43,
    height: 43,
    borderRadius: 26,
    justifyContent: "center",
    alignItems: "center",
    overflow: "hidden",
  },
  miniLogoInnerContainer: {
    width: 38,
    height: 38,
    borderRadius: 19,
    justifyContent: "center",
    alignItems: "center",
    position: "relative",
    overflow: "hidden",
  },
  miniLogo: {
    width: 24,
    height: 24,
    zIndex: 2,
  },
  logoutButton: {
    flexDirection: "row",
    alignItems: "center",
    marginTop: 50,
  },
  logoutText: {
    fontSize: 16,
    fontFamily: "RedRose_500Medium",
    color: "#FF0000",
    marginLeft: 20,
  },
  switchContainer: {
    width: 50,
    height: 30,
    borderRadius: 15,
    padding: 3,
  },
  switchCircle: {
    width: 24,
    height: 24,
    borderRadius: 12,
  },
});