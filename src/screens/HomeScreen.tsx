//@ts-nocheck
import React, { useState, useRef, useEffect } from 'react';
import { View, Text, StyleSheet, Image, TouchableOpacity, FlatList, Dimensions, useColorScheme } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { COLORS } from '../constants/colors';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { FontAwesome6 } from '@expo/vector-icons';
import { Video, X, RectangleHorizontal, ArrowUp, Captions, CaptionsOff, AudioLines } from 'lucide-react-native';
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withTiming,
  withSpring
} from 'react-native-reanimated';
import Svg, { Path } from 'react-native-svg';

const { width, height } = Dimensions.get('window');

{ /*Subcomponent: Animated Waveform (SVG) */ }
function Waveform({ show, paused }) {
  const amplitude = useSharedValue(show && !paused ? 1 : 0);
  useEffect(() => {
    amplitude.value = withTiming((show && !paused) ? 1 : 0, { duration: 400 });
  }, [show, paused]);


  const style = useAnimatedStyle(() => ({
    opacity: amplitude.value,
    transform: [{ scaleY: amplitude.value }],
  }));

  return (
    <Animated.View style={[styles.waveform, style]}>
      <Svg width={210} height={60}>
        {/* Blue Wave - Top */}
        <Path
          d="M0,30 Q50,5 100,30 T200,30"
          stroke="#4285f4"
          strokeWidth={7}
          fill="none"
          opacity={0.6}
        />
        {/* Red Wave - Bottom */}
        <Path
          d="M0,50 Q50,75 100,50 T200,50"
          stroke="#e63946"
          strokeWidth={7}
          fill="none"
          opacity={0.6}
        />
      </Svg>
      {/* Layer blur styles (or apply blur via layer if available) */}
      <View style={styles.waveBlur} />
    </Animated.View>
  );
}

// Subcomponent: Pill Control Buttons
function PillsControls({ show, paused, onPause, onStop, onShare, onCamera }) {
  const translateY = useSharedValue(show ? 0 : 100);
  useEffect(() => {
    translateY.value = withSpring(show ? 0 : 100, { damping: 18, stiffness: 90 });
  }, [show]);
  const animStyle = useAnimatedStyle(() => ({
    transform: [{ translateY: translateY.value }],
    opacity: show ? 1 : 0,
  }));

  return (
    <Animated.View style={[styles.pillControlsWrapper, animStyle]}>
      {/* Pause/Resume */}
      <TouchableOpacity style={styles.pillBtn} onPress={onPause}>
        {paused ? (
          <AudioLines size={27} color="#4285f4" />
        ) : (
          <FontAwesome6 name="pause" size={27} color="#4285f4" />
        )}
      </TouchableOpacity>
      {/* Screen share: Rectangle + ArrowUp inside */}
      <TouchableOpacity style={styles.pillBtn} onPress={onShare}>
        <View style={{ position: 'relative', width: 32, height: 27, justifyContent: 'center', alignItems: 'center' }}>
          <RectangleHorizontal size={32} color="#4285f4" />
          <ArrowUp size={18} color="#4285f4" style={{ position: 'absolute', alignSelf: 'center', top: 5 }} />
        </View>
      </TouchableOpacity>
      {/* Camera */}
      <TouchableOpacity style={styles.pillBtn} onPress={onCamera}>
        <Video size={27} color="#4285f4" />
      </TouchableOpacity>
      {/* Stop */}
      <TouchableOpacity style={[styles.pillBtn, styles.pillBtnStop]} onPress={onStop}>
        <X size={27} color="#fff" />
      </TouchableOpacity>
    </Animated.View>
  );
}

//Captions List with Fade Edges 
function CaptionsList({ visible, captions }) {
  const opacity = useSharedValue(visible ? 1 : 0);
  useEffect(() => {
    opacity.value = withTiming(visible ? 1 : 0, { duration: 350 });
  }, [visible]);
  const fadeAnim = useAnimatedStyle(() => ({ opacity: opacity.value }));

  return (
    <Animated.View style={[styles.captionsContainer, fadeAnim]}>
      <LinearGradient colors={['#000', 'transparent']} style={styles.fadeEdgeTop} />
      <FlatList
        data={captions}
        renderItem={({ item }) => <Text style={styles.captionText}>{item}</Text>}
        keyExtractor={(_, i) => `caption-${i}`}
        style={styles.flatList}
        showsVerticalScrollIndicator={false}
      />
      <LinearGradient colors={['transparent', '#000']} style={styles.fadeEdgeBottom} />
    </Animated.View>
  );
}

// MAIN HOMESCREEN COMPONENT
export default function HomeScreen({ navigation }) {
  const colorScheme = useColorScheme();
  const isDark = colorScheme === 'dark';
  const colors = isDark ? COLORS.dark : COLORS.light;
  const insets = useSafeAreaInsets();

  // UI States
  const [listeningMode, setListeningMode] = useState(false);
  const [captionsEnabled, setCaptionsEnabled] = useState(false);
  const [paused, setPaused] = useState(false);

  // Animated container scaling/transitions
  const headerOpacity = useSharedValue(1);
  const logoScale = useSharedValue(1);
  const newChatOpacity = useSharedValue(0);
  const captionsIconOpacity = useSharedValue(0);
  const startTextOpacity = useSharedValue(1);
  const circleY = useSharedValue(0);
  const circleScale = useSharedValue(1);

  useEffect(() => {
    headerOpacity.value = withTiming(listeningMode ? 0 : 1, { duration: 400 });
    logoScale.value = withSpring(listeningMode ? 0.82 : 1, { damping: 20 });
    startTextOpacity.value = withTiming(listeningMode ? 0 : 1, { duration: 380 });
    newChatOpacity.value = withTiming(listeningMode ? 1 : 0, { duration: 420 });
    captionsIconOpacity.value = withTiming(listeningMode ? 1 : 0, { duration: 420 });
  }, [listeningMode]);

  useEffect(() => {
    circleScale.value = withTiming(captionsEnabled ? 0.7 : 1, { duration: 300 });
    circleY.value = withSpring(captionsEnabled ? -80 : 0, { damping: 17 });
  }, [captionsEnabled]);

  // Animated styles
  const headerAnimStyle = useAnimatedStyle(() => ({ opacity: headerOpacity.value }));
  const logoAnimStyle = useAnimatedStyle(() => ({
    transform: [{ scale: logoScale.value }, { translateY: circleY.value }],
  }));
  const newChatAnimStyle = useAnimatedStyle(() => ({ opacity: newChatOpacity.value }));
  const captionsIconAnimStyle = useAnimatedStyle(() => ({ opacity: captionsIconOpacity.value }));
  const startTextAnimStyle = useAnimatedStyle(() => ({ opacity: startTextOpacity.value }));
  const circleAnimStyle = useAnimatedStyle(() => ({
    transform: [{ scale: circleScale.value }, { translateY: circleY.value }]
  }));

  // Captions data
  const captionsData = [
    "Einstein’s field equations are the core of Einstein’s general theory of relativity.",
    "They describe how matter and energy in the universe curve the fabric of spacetime.",
    "The equations are a set of ten interrelated differential equations.",
    "Ivory is listening and transcribing…"
  ];

  // UI Event handlers
  const handleStartListening = () => setListeningMode(true);
  const handlePause = () => setPaused((p) => !p);
  const handleStop = () => {
    setListeningMode(false);
    setPaused(false);
    setCaptionsEnabled(false);
  };
  const handleShare = () => { }; 
  const handleCamera = () => { };
  const handleToggleCaptions = () => setCaptionsEnabled(c => !c);

  return (
    <View style={[styles.container, { backgroundColor: colors.background }]}>
      {/* Top Avatar */}
      <Animated.View style={[styles.avatarContainer, headerAnimStyle, { top: insets.top + 10 }]}>
        <TouchableOpacity onPress={() => navigation.openDrawer()}>
          <Image source={require('../../assets/20250805_160609.jpg')} style={styles.avatar} />
        </TouchableOpacity>
      </Animated.View>
      {/* Top Center Brand */}
      <Animated.View style={[styles.brand, headerAnimStyle, { top: insets.top + 15 }]}>
        <Image source={require("../../assets/ivory.png")} style={styles.brandImage} resizeMode="contain" />
      </Animated.View>
      {/* Captions Icon Top Right */}
      {listeningMode && (
        <Animated.View style={[styles.captionsIcon, captionsIconAnimStyle, { top: insets.top + 10 }]}>
          <TouchableOpacity onPress={handleToggleCaptions}>
            {captionsEnabled ? (
              <CaptionsOff size={26} color="#4285f4" />
            ) : (
              <Captions size={26} color="#4285f4" />
            )}
          </TouchableOpacity>
        </Animated.View>
      )}
      {/* Main Center Content */}
      <View style={styles.centerContent}>
        <Animated.View style={[styles.shadowContainer, logoAnimStyle, circleAnimStyle]}>
          <LinearGradient
            colors={['#e63946', '#4285f4']}
            start={{ x: 0, y: 0 }} end={{ x: 1, y: 1 }}
            style={styles.gradientBorder}
          >
            <TouchableOpacity onPress={handleStartListening}>
              <View style={[styles.logoInnerContainer, { backgroundColor: colors.background }]}>
                <Image source={require('../../assets/ivorystar.png')} style={styles.logo} />
                <Waveform show={listeningMode && !paused} paused={paused} />
              </View>
            </TouchableOpacity>

          </LinearGradient>
        </Animated.View>
        {/* Start Text */}
        {!listeningMode && (
          <Animated.View style={[startTextAnimStyle]}>
            <TouchableOpacity activeOpacity={0.7} onPress={handleStartListening}>
              <Text style={[styles.startText, { color: colors.text }]}>Tap to start ivory</Text>
            </TouchableOpacity>
          </Animated.View>
        )}
      </View>
      {/* New Chat Header Text */}
      {listeningMode && (
        <Animated.View style={[styles.newChatHeader, newChatAnimStyle]}>
          <Text style={styles.newChatText}>New Chat</Text>
        </Animated.View>
      )}
      {/* Pills Controls */}
      <PillsControls
        show={listeningMode}
        paused={paused}
        onPause={handlePause}
        onStop={handleStop}
        onShare={handleShare}
        onCamera={handleCamera}
      />
      {/* Captions FlatList */}
      {listeningMode && (
        <CaptionsList visible={captionsEnabled} captions={captionsData} />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  avatarContainer: {
    position: 'absolute',
    left: 18,
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
    zIndex: 9,
  },
  brandImage: {
    height: 35,
    width: 120,
  },
  captionsIcon: {
    position: 'absolute',
    right: 22,
    zIndex: 999,
  },
  centerContent: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  shadowContainer: {
    marginBottom: 44,
    shadowColor: '#4285f4',
    shadowOffset: { width: 5, height: 20 },
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
    overflow: 'hidden',
  },
  logoInnerContainer: {
    width: 228,
    height: 228,
    borderRadius: 134,
    justifyContent: 'center',
    alignItems: 'center',
    position: 'relative',
  },
  logo: {
    width: 150,
    height: 150,
  },
  startText: {
    fontSize: 25,
    fontFamily: 'RedRose_600SemiBold',
    textAlign: 'center',
    marginTop: 18,
  },
  newChatHeader: {
    position: 'absolute',
    top: 50,
    left: 0,
    right: 0,
    alignItems: 'center',
    zIndex: 2,
  },
  newChatText: {
    fontSize: 27,
    fontWeight: '700',
    color: '#4285f4',
    fontFamily: 'RedRose_600SemiBold',
  },
  pillControlsWrapper: {
    position: 'absolute',
    left: 0,
    right: 0,
    bottom: 40,
    flexDirection: 'row',
    justifyContent: 'space-evenly',
    alignItems: 'center',
    zIndex: 300,
    paddingHorizontal: 26,
  },
  pillBtn: {
    flexDirection: 'row',
    backgroundColor: "#f3f6fa",
    paddingVertical: 10,
    paddingHorizontal: 22,
    borderRadius: 28,
    elevation: 8,
    alignItems: 'center',
    marginHorizontal: 3,
    borderWidth: 1.2,
    borderColor: "#cacacaff",
  },
  pillBtnStop: {
    backgroundColor: "#e63946",
    borderColor: "#e63946"
  },
  waveform: {
    position: 'absolute',
    bottom: 25,
    left: 3,
    right: 0,
    height: 60,
    width: 210,
    alignItems: 'center',
    justifyContent: 'center',
    overflow: 'visible',
  },
  waveBlur: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'transparent',
  },
  captionsContainer: {
    position: 'absolute',
    top: height * 0.14,
    left: width * 0.13,
    right: width * 0.13,
    bottom: height * 0.20,
    zIndex: 110,
    pointerEvents: 'box-none'
  },
  fadeEdgeTop: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    height: 26,
    zIndex: 12,
  },
  fadeEdgeBottom: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    height: 26,
    zIndex: 12,
  },
  flatList: {
    flex: 1,
    paddingVertical: 38,
  },
  captionText: {
    fontSize: 20,
    color: "#313445",
    textAlign: 'center',
    marginVertical: 10,
    fontFamily: 'RedRose_600SemiBold',
  },
});
