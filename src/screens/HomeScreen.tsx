import React, { useState, useRef, useEffect } from "react";
import {
	View,
	Text,
	StyleSheet,
	Image,
	TouchableOpacity,
	Dimensions,
	useColorScheme,
	ScrollView,
} from "react-native";
import { LinearGradient } from "expo-linear-gradient";
import { COLORS } from "../constants/colors";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { FontAwesome6 } from "@expo/vector-icons";
import {
	Video,
	X,
	RectangleHorizontal,
	ArrowUp,
	Captions,
	CaptionsOff,
	AudioLines,
} from "lucide-react-native";
import Animated, {
	useSharedValue,
	useAnimatedStyle,
	withTiming,
	withSpring,
	withRepeat,
	Easing,
	interpolate,
	Extrapolate,
	useAnimatedProps,
} from "react-native-reanimated";
import Svg, {
	Path,
	Defs,
	LinearGradient as SvgGradient,
	Stop,
} from "react-native-svg";
import { useGeminiLive } from "../contexts/GeminiLiveContext";

const { width, height } = Dimensions.get("window");

// Subcomponent: Animated Waveform with flowing motion
function Waveform({ show, paused }: { show: boolean; paused: boolean }) {
	const amplitude = useSharedValue(show && !paused ? 1 : 0);
	const phase = useSharedValue(0);
	const waveHeight = useSharedValue(show && !paused ? 1 : 0);

	useEffect(() => {
		if (show && !paused) {
			waveHeight.value = withSpring(1, { damping: 15, stiffness: 80 });
			amplitude.value = withTiming(1, { duration: 600, easing: Easing.ease });
			phase.value = withRepeat(
				withTiming(2 * Math.PI, { duration: 3000, easing: Easing.linear }),
				-1,
				false
			);
		} else {
			waveHeight.value = withTiming(0, { duration: 400 });
			amplitude.value = withTiming(0, { duration: 400 });
			phase.value = 0;
		}
	}, [show, paused]);

	const AnimatedPath = Animated.createAnimatedComponent(Path);

	const animatedProps = useAnimatedProps(() => {
		const A = 25 * amplitude.value;
		const λ = width / 1.0;
		const φ = phase.value;

		const baseY = interpolate(
			waveHeight.value,
			[0, 1],
			[250, 80],
			Extrapolate.CLAMP
		);

		let path = `M 0 ${baseY}`;
		for (let x = 0; x <= width; x += 10) {
			const y = baseY + A * Math.sin((2 * Math.PI * x) / λ + φ);
			path += ` L ${x} ${y}`;
		}
		path += ` L ${width} 200 L 0 200 Z`;

		return { d: path };
	});

	const animatedProps2 = useAnimatedProps(() => {
		const A = 18 * amplitude.value;
		const λ = width / 1.1;
		const φ = phase.value + Math.PI / 2;

		const baseY = interpolate(
			waveHeight.value,
			[0, 1],
			[255, 90],
			Extrapolate.CLAMP
		);

		let path = `M 0 ${baseY}`;
		for (let x = 0; x <= width; x += 10) {
			const y = baseY + A * Math.sin((2 * Math.PI * x) / λ + φ);
			path += ` L ${x} ${y}`;
		}
		path += ` L ${width} 200 L 0 200 Z`;
		return { d: path };
	});

	return (
		<View style={styles.waveformContainer}>
			<Svg width={width} height={200}>
				<Defs>
					<SvgGradient id="waveGrad1" x1="0" y1="0" x2="1" y2="1">
						<Stop offset="0%" stopColor="#4285f4" stopOpacity="0.8" />
						<Stop offset="100%" stopColor="#6ea8fe" stopOpacity="0.2" />
					</SvgGradient>
					<SvgGradient id="waveGrad2" x1="0" y1="0" x2="1" y2="1">
						<Stop offset="0%" stopColor="#EE3585" stopOpacity="0.8" />
						<Stop offset="100%" stopColor="#ff7b89" stopOpacity="0.2" />
					</SvgGradient>
				</Defs>

				<AnimatedPath animatedProps={animatedProps} fill="url(#waveGrad1)" />
				<AnimatedPath animatedProps={animatedProps2} fill="url(#waveGrad2)" />
			</Svg>
		</View>
	);
}

// Subcomponent: Pill Control Buttons
function PillsControls({
	show,
	paused,
	onPause,
	onStop,
	onShare,
	onCamera,
}: {
	show: boolean;
	paused: boolean;
	onPause: () => void;
	onStop: () => void;
	onShare: () => void;
	onCamera: () => void;
}) {
	const translateY = useSharedValue(show ? 0 : 100);

	useEffect(() => {
		translateY.value = withSpring(show ? 0 : 100, {
			damping: 18,
			stiffness: 90,
		});
	}, [show]);

	const animStyle = useAnimatedStyle(() => ({
		transform: [{ translateY: translateY.value }],
		opacity: show ? 1 : 0,
	}));
	const colorScheme = useColorScheme();
	const isDark = colorScheme === "dark";
	const colors = isDark ? COLORS.dark : COLORS.light;

	return (
		<Animated.View style={[styles.pillControlsWrapper, animStyle]}>
			{/* Pause/Resume */}
			<TouchableOpacity
				style={[
					styles.pillBtn,
					{
						backgroundColor: isDark ? "#52525257" : "#41414111",
						elevation: isDark ? 8 : 0,
					},
				]}
				onPress={onPause}
			>
				{paused ? (
					<AudioLines
						size={24}
						color={isDark ? "#ffffff" : "#000000"}
					/>
				) : (
					<FontAwesome6
						name="pause"
						size={24}
						color={isDark ? "#ffffff" : "#000000"}
						style={{ marginHorizontal: 4.5 }}
					/>
				)}
			</TouchableOpacity>

			{/* Screen share: Rectangle + ArrowUp inside */}
			<TouchableOpacity
				style={[
					styles.pillBtn,
					{
						backgroundColor: isDark ? "#52525257" : "#41414111",
						elevation: isDark ? 8 : 0,
					},
				]}
				onPress={onShare}
			>
				<View style={styles.screenShareIcon}>
					<RectangleHorizontal
						size={32}
						color={isDark ? "#ffffff" : "#000000"}
					/>
					<ArrowUp
						size={14}
						color={isDark ? "#ffffff" : "#000000"}
						style={styles.arrowUpIcon}
					/>
				</View>
			</TouchableOpacity>

			{/* Camera */}
			<TouchableOpacity
				style={[
					styles.pillBtn,
					{
						backgroundColor: isDark ? "#52525257" : "#41414111",
						elevation: isDark ? 8 : 0,
					},
				]}
				onPress={onCamera}
			>
				<Video
					size={24}
					color={isDark ? "#ffffff" : "#000000"}
					style={{ marginHorizontal: 2 }}
				/>
			</TouchableOpacity>

			{/* Stop */}
			<TouchableOpacity
				style={[styles.pillBtn, styles.pillBtnStop]}
				onPress={onStop}
			>
				<X size={24} color="#fff" />
			</TouchableOpacity>
		</Animated.View>
	);
}

// Captions with continuous flow
function CaptionsList({
	visible,
	captions,
}: {
	visible: boolean;
	captions: string[];
}) {
	const scrollViewRef = useRef<ScrollView>(null);
	const opacity = useSharedValue(visible ? 1 : 0);

	const colorScheme = useColorScheme();
	const isDark = colorScheme === "dark";
	const colors = isDark ? COLORS.dark : COLORS.light;

	useEffect(() => {
		opacity.value = withTiming(visible ? 1 : 0, { duration: 350 });
	}, [visible]);

	useEffect(() => {
		if (visible && scrollViewRef.current && captions.length > 0) {
			setTimeout(() => {
				scrollViewRef.current?.scrollToEnd({ animated: true });
			}, 100);
		}
	}, [captions, visible]);

	const fadeAnim = useAnimatedStyle(() => ({ opacity: opacity.value }));

	const captionsText = captions.join(" ");

	return (
		<Animated.View
			style={[styles.captionsContainer, fadeAnim]}
			pointerEvents={visible ? "auto" : "none"}
		>
			<LinearGradient
				colors={[colors.background, "transparent"]}
				style={styles.fadeEdgeTop}
			/>
			<ScrollView
				ref={scrollViewRef}
				style={styles.flatList}
				contentContainerStyle={styles.flatListContent}
				showsVerticalScrollIndicator={false}
			>
				<Text style={[styles.captionText, { color: colors.text }]}>
					{captionsText}
				</Text>
			</ScrollView>
			<LinearGradient
				colors={["transparent", colors.background]}
				style={styles.fadeEdgeBottom}
			/>
		</Animated.View>
	);
}

// MAIN HOMESCREEN COMPONENT
export default function HomeScreen({ navigation }: { navigation: any }) {
	const colorScheme = useColorScheme();
	const isDark = colorScheme === "dark";
	const colors = isDark ? COLORS.dark : COLORS.light;
	const insets = useSafeAreaInsets();

	// UI States
	const [listeningMode, setListeningMode] = useState(false);
	const [captionsEnabled, setCaptionsEnabled] = useState(false);
	const [paused, setPaused] = useState(false);

	// Animated values
	const headerOpacity = useSharedValue(1);
	const avatarOpacity = useSharedValue(1);
	const newChatOpacity = useSharedValue(0);
	const captionsIconOpacity = useSharedValue(0);
	const startTextOpacity = useSharedValue(1);
	const pauseTextOpacity = useSharedValue(0);
	const circleY = useSharedValue(0);
	const circleScale = useSharedValue(1);
	const circleBounce = useSharedValue(0);

	useEffect(() => {
		headerOpacity.value = withTiming(listeningMode ? 0 : 1, { duration: 400 });
		avatarOpacity.value = withTiming(listeningMode ? 0 : 1, { duration: 400 });
		startTextOpacity.value = withTiming(listeningMode ? 0 : 1, {
			duration: 380,
		});
		newChatOpacity.value = withTiming(listeningMode ? 1 : 0, { duration: 420 });
		captionsIconOpacity.value = withTiming(listeningMode ? 1 : 0, {
			duration: 420,
		});
	}, [listeningMode]);

	useEffect(() => {
		pauseTextOpacity.value = withTiming(paused ? 1 : 0, { duration: 300 });
	}, [paused]);

	useEffect(() => {
		if (captionsEnabled && !paused) {
			circleScale.value = withSpring(0.55, { damping: 17, stiffness: 100 });
			circleY.value = withSpring(-height * 0.54, {
				damping: 17,
				stiffness: 100,
			});
		} else {
			circleScale.value = withSpring(1, { damping: 17, stiffness: 100 });
			circleY.value = withSpring(0, { damping: 17, stiffness: 100 });
		}
	}, [captionsEnabled, paused]);

	// Animated styles
	const headerAnimStyle = useAnimatedStyle(() => ({
		opacity: headerOpacity.value,
	}));
	const avatarAnimStyle = useAnimatedStyle(() => ({
		opacity: avatarOpacity.value,
	}));
	const newChatAnimStyle = useAnimatedStyle(() => ({
		opacity: newChatOpacity.value,
	}));
	const captionsIconAnimStyle = useAnimatedStyle(() => ({
		opacity: captionsIconOpacity.value,
	}));
	const startTextAnimStyle = useAnimatedStyle(() => ({
		opacity: startTextOpacity.value,
	}));
	const pauseTextAnimStyle = useAnimatedStyle(() => ({
		opacity: pauseTextOpacity.value,
	}));
	const circleAnimStyle = useAnimatedStyle(() => ({
		transform: [
			{ scale: circleScale.value },
			{ translateY: circleY.value + circleBounce.value },
		],
	}));

	// Captions data
	const captionsData = [
		"Einstein's field equations are the core of Einstein's general theory of relativity.",
		"They describe how matter and energy in the universe curve the fabric of spacetime.",
		"The equations are a set of ten interrelated differential equations.",
		"The curvature of spacetime is directly related to the energy and momentum of whatever is present.",
		"These equations form the foundation of our modern understanding of gravity and cosmology.",
	];

	// UI Event handlers
	const handleStartListening = () => {
		if (!listeningMode) {
			// Bounce animation when activated
			circleBounce.value = withSpring(
				-15,
				{ damping: 10, stiffness: 200 },
				() => {
					circleBounce.value = withSpring(0, { damping: 12, stiffness: 150 });
				}
			);
			setListeningMode(true);
		}
	};

	const handlePause = () => setPaused((p) => !p);

	const handleStop = () => {
		// Bounce down when deactivated
		circleBounce.value = withSpring(10, { damping: 10, stiffness: 200 }, () => {
			circleBounce.value = withSpring(0, { damping: 12, stiffness: 150 });
		});

		setListeningMode(false);
		setPaused(false);
		setCaptionsEnabled(false);
	};

	const handleShare = () => {
		console.log("Screen share pressed");
	};

	const handleCamera = () => {
		console.log("Camera pressed");
	};

	const handleToggleCaptions = () => setCaptionsEnabled((c) => !c);

	return (
		<View style={[styles.container, { backgroundColor: colors.background }]}>
			{/* Top Avatar */}
			<Animated.View
				style={[
					styles.avatarContainer,
					avatarAnimStyle,
					{ top: insets.top + 10 },
				]}
			>
				<TouchableOpacity onPress={() => navigation.openDrawer()}>
					<Image
						source={require("../../assets/20250805_160609.jpg")}
						style={styles.avatar}
					/>
				</TouchableOpacity>
			</Animated.View>

			{/* Top Center Brand */}
			<Animated.View
				style={[styles.brand, headerAnimStyle, { top: insets.top + 15 }]}
			>
				<Image
					source={require("../../assets/ivory.png")}
					style={styles.brandImage}
					resizeMode="contain"
				/>
			</Animated.View>

			{/* New Chat Header Text */}
			<Animated.View
				style={[
					styles.newChatHeader,
					newChatAnimStyle,
					{ top: insets.top + 15 },
				]}
			>
				<Text style={[styles.newChatText, { color: colors.text }]}>
					New Chat
				</Text>
			</Animated.View>

			{/* Captions Icon Top Right */}
			<Animated.View
				style={[
					styles.captionsIcon,
					captionsIconAnimStyle,
					{ top: insets.top + 18 },
				]}
			>
				<TouchableOpacity onPress={handleToggleCaptions}>
					{captionsEnabled ? (
						<CaptionsOff size={26} color={isDark ? "#FFFFFF" : "#000000"} />
					) : (
						<Captions size={26} color={isDark ? "#FFFFFF" : "#000000"} />
					)}
				</TouchableOpacity>
			</Animated.View>

			{/* Main Center Content */}
			<View style={styles.centerContent}>
				<Animated.View
					style={[
						styles.shadowContainer,
						circleAnimStyle,
						{ elevation: captionsEnabled ? 0 : 40 },
					]}
				>
					<LinearGradient
						colors={["#e63946", "#4285f4"]}
						start={{ x: 0, y: 0 }}
						end={{ x: 1, y: 1 }}
						style={styles.gradientBorder}
					>
						<TouchableOpacity
							onPress={handleStartListening}
							activeOpacity={0.8}
						>
							<View
								style={[
									styles.logoInnerContainer,
									{ backgroundColor: colors.background },
								]}
							>
								<Image
									source={require("../../assets/ivorystar.png")}
									style={styles.logo}
								/>
								<Waveform show={listeningMode} paused={paused} />
							</View>
						</TouchableOpacity>
					</LinearGradient>

					{/* Pause Text Overlay */}
					{listeningMode && paused && (
						<Animated.View
							style={[styles.pauseTextContainer, pauseTextAnimStyle]}
						>
							<Text style={[styles.pauseText, { color: colors.text }]}>
								ivory is on hold
							</Text>
						</Animated.View>
					)}
				</Animated.View>

				{/* Start Text */}
				{!listeningMode && (
					<Animated.View style={[startTextAnimStyle]}>
						<TouchableOpacity
							activeOpacity={0.7}
							onPress={handleStartListening}
						>
							<Text style={[styles.startText, { color: colors.text }]}>
								Tap to start ivory
							</Text>
						</TouchableOpacity>
					</Animated.View>
				)}
			</View>

			{/* Pills Controls */}
			<PillsControls
				show={listeningMode}
				paused={paused}
				onPause={handlePause}
				onStop={handleStop}
				onShare={handleShare}
				onCamera={handleCamera}
			/>

			{/* Captions */}
			<CaptionsList
				visible={listeningMode && captionsEnabled && !paused}
				captions={captionsData}
			/>
		</View>
	);
}

const styles = StyleSheet.create({
	container: {
		flex: 1,
	},
	avatarContainer: {
		position: "absolute",
		left: 18,
		zIndex: 10,
	},
	avatar: {
		width: 36,
		height: 36,
		borderRadius: 18,
		backgroundColor: "#ddd",
	},
	brand: {
		position: "absolute",
		alignSelf: "center",
		height: 35,
		width: 120,
		zIndex: 9,
	},
	brandImage: {
		height: 35,
		width: 120,
	},
	newChatHeader: {
		position: "absolute",
		alignSelf: "center",
		zIndex: 9,
	},
	newChatText: {
		fontSize: 18,
		fontFamily: "RedRose_500Medium",
	},
	captionsIcon: {
		position: "absolute",
		right: 22,
		zIndex: 999,
	},
	centerContent: {
		flex: 1,
		justifyContent: "center",
		alignItems: "center",
	},
	shadowContainer: {
		marginBottom: 44,
		shadowColor: "#4285f4",
		shadowOffset: { width: 5, height: 20 },
		shadowOpacity: 1,
		shadowRadius: 16,
		elevation: 40,
		borderRadius: 140,
    zIndex: 200
	},
	gradientBorder: {
		width: 245,
		height: 245,
		borderRadius: 190,
		justifyContent: "center",
		alignItems: "center",
		overflow: "hidden",
	},
	logoInnerContainer: {
		width: 228,
		height: 228,
		borderRadius: 134,
		justifyContent: "center",
		alignItems: "center",
		position: "relative",
		overflow: "hidden",
	},
	logo: {
		width: 150,
		height: 150,
		zIndex: 2,
	},
	waveformContainer: {
		position: "absolute",
		bottom: 0,
		left: 0,
		right: 0,
		height: 200,
		overflow: "hidden",
	},
	pauseTextContainer: {
		position: "absolute",
		zIndex: 10,
		bottom: -60,
		width: 228,
		alignItems: "center",
		left: 10,
	},
	pauseText: {
		fontSize: 25,
		top: 10,
		fontFamily: "RedRose_400Regular",
		textAlign: "center",
	},
	startText: {
		fontSize: 25,
		fontFamily: "RedRose_600SemiBold",
		textAlign: "center",
		marginTop: 18,
	},
	pillControlsWrapper: {
		position: "absolute",
		left: 0,
		right: 0,
		bottom: 40,
		flexDirection: "row",
		justifyContent: "center",
		alignItems: "center",
		zIndex: 300,
		paddingHorizontal: 26,
		gap: 10,
	},
	pillBtn: {
		flexDirection: "row",
		backgroundColor: "#f3f6fa",
		paddingVertical: 12,
		paddingHorizontal: 20,
		borderRadius: 28,
		elevation: 8,
		alignItems: "center",
		justifyContent: "center",
		borderWidth: 1.2,
		borderColor: "#5252521e",
		minWidth: 60,
		height: 50,
	},
	pillBtnStop: {
		backgroundColor: "#e63946",
		borderColor: "#e63946",
	},
	screenShareIcon: {
		position: "relative",
		width: 32,
		height: 24,
		justifyContent: "center",
		alignItems: "center",
	},
	arrowUpIcon: {
		position: "absolute",
		top: 5,
	},
	captionsContainer: {
		position: "absolute",
		top: height * 0.3,
		left: width * 0.01,
		right: width * 0.01,
		bottom: height * 0.18,
		zIndex: 110,
	},
	fadeEdgeTop: {
		position: "absolute",
		top: 0,
		left: 0,
		right: 0,
		height: 40,
		zIndex: 12,
		pointerEvents: "none",
	},
	fadeEdgeBottom: {
		position: "absolute",
		bottom: -2,
		left: 0,
		right: 0,
		height: 40,
		zIndex: 12,
		pointerEvents: "none",
	},
	flatList: {
		flex: 1,
	},
	flatListContent: {
		paddingVertical: 20,
		paddingHorizontal: 20,
	},
	captionText: {
		fontSize: 25,
		textAlign: "auto",
		fontFamily: "RedRose_400Regular",
		lineHeight: 32,
	},
});
