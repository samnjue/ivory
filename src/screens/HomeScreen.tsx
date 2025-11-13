import React, { useState, useRef, useEffect, useCallback } from "react";
import {
	View,
	Text,
	StyleSheet,
	Image,
	TouchableOpacity,
	Dimensions,
	useColorScheme,
	ScrollView,
	Alert,
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
import { Audio } from "expo-av";
import * as Speech from "expo-speech";

const { width, height } = Dimensions.get("window");

const DEEPGRAM_API_KEY = "5e3a3494c6e466ee6b9ca25c925cc442bff71f76";
const DEEPGRAM_WS_URL = `wss://api.deepgram.com/v1/listen?model=nova-2&language=en&interim_results=true&punctuate=true&smart_format=true`;

const GEMINI_API_KEY = "AIzaSyBVBUbRke_aAwNiwGnu5CrxzbCRWkHrrrE";
const GEMINI_API_URL = `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${GEMINI_API_KEY}`;

const SAMPLE_RATE = 16000;
const CHANNELS = 1;
const BIT_DEPTH = 16;

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

	return (
		<Animated.View style={[styles.pillControlsWrapper, animStyle]}>
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
					<AudioLines size={24} color={isDark ? "#ffffff" : "#000000"} />
				) : (
					<FontAwesome6
						name="pause"
						size={24}
						color={isDark ? "#ffffff" : "#000000"}
						style={{ marginHorizontal: 4.5 }}
					/>
				)}
			</TouchableOpacity>

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

			<TouchableOpacity
				style={[styles.pillBtn, styles.pillBtnStop]}
				onPress={onStop}
			>
				<X size={24} color="#fff" />
			</TouchableOpacity>
		</Animated.View>
	);
}

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
		if (visible && scrollViewRef.current && captions.length) {
			setTimeout(
				() => scrollViewRef.current?.scrollToEnd({ animated: true }),
				100
			);
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

export default function HomeScreen({ navigation }: { navigation: any }) {
	const colorScheme = useColorScheme();
	const isDark = colorScheme === "dark";
	const colors = isDark ? COLORS.dark : COLORS.light;
	const insets = useSafeAreaInsets();

	const [listeningMode, setListeningMode] = useState(false);
	const [captionsEnabled, setCaptionsEnabled] = useState(false);
	const [paused, setPaused] = useState(false);
	const [captions, setCaptions] = useState<string[]>([]);

	const recordingRef = useRef<Audio.Recording | null>(null);
	const wsRef = useRef<WebSocket | null>(null);
	const chunkTimer = useRef<NodeJS.Timeout | null>(null);
	const isProcessing = useRef(false);

	const callGemini = async (text: string): Promise<string | null> => {
		if (isProcessing.current) return null;
		isProcessing.current = true;

		try {
			const res = await fetch(GEMINI_API_URL, {
				method: "POST",
				headers: { "Content-Type": "application/json" },
				body: JSON.stringify({
					contents: [{ role: "user", parts: [{ text }] }],
				}),
			});
			const data = await res.json();
			return data.candidates?.[0]?.content?.parts?.[0]?.text?.trim() || null;
		} catch (err) {
			console.error("Gemini error:", err);
			return null;
		} finally {
			isProcessing.current = false;
		}
	};

	const speakResponse = async (text: string) => {
		await Speech.speak(text, {
			language: "en-US",
			rate: 0.95,
			pitch: 1.0,
		});
	};

	const startRecording = useCallback(async () => {
		try {
			console.log("Starting recording...");
			const { granted } = await Audio.requestPermissionsAsync();
			if (!granted) {
				Alert.alert("Error", "Microphone permission denied.");
				handleStop();
				return;
			}

			await Audio.setAudioModeAsync({
				allowsRecordingIOS: true,
				playsInSilentModeIOS: true,
			});

			const recording = new Audio.Recording();
			await recording.prepareToRecordAsync({
				android: {
					extension: ".wav",
					outputFormat: Audio.AndroidOutputFormat.DEFAULT,
					audioEncoder: Audio.AndroidAudioEncoder.DEFAULT,
					sampleRate: SAMPLE_RATE,
					numberOfChannels: CHANNELS,
					bitRate: 128000,
				},
				ios: {
					extension: ".wav",
					audioQuality: Audio.IOSAudioQuality.HIGH,
					sampleRate: SAMPLE_RATE,
					numberOfChannels: CHANNELS,
					bitRate: 128000,
					linearPCMBitDepth: BIT_DEPTH,
					linearPCMIsBigEndian: false,
					linearPCMIsFloat: false,
				},
				web: {
					mimeType: "audio/webm",
					bitsPerSecond: 128000,
				},
			});

			await recording.startAsync();
			recordingRef.current = recording;
			console.log("Recording started");

			chunkTimer.current = setInterval(() => {
				if (
					!recordingRef.current ||
					!wsRef.current ||
					paused ||
					wsRef.current.readyState !== WebSocket.OPEN
				)
					return;

				const uri = recordingRef.current?.getURI();
				if (!uri) return;

				fetch(uri)
					.then((res) => res.arrayBuffer())
					.then((buf) => {
						const pcm = new Uint8Array(buf);
						if (pcm.length > 0) {
							wsRef.current?.send(pcm);
						}
					})
					.catch(console.error);
			}, 100);
		} catch (e) {
			console.error("Recording error:", e);
			Alert.alert("Error", `Failed to start recording: ${e}`);
			handleStop();
		}
	}, [paused]);

	const startDeepgram = useCallback(() => {
		if (wsRef.current) return;

		console.log("Connecting to Deepgram...");
		const ws = new WebSocket(DEEPGRAM_WS_URL);
		wsRef.current = ws;
		ws.binaryType = "arraybuffer";

		ws.onopen = () => {
			console.log("Deepgram connected");
			startRecording();
		};

		ws.onmessage = async (event) => {
			try {
				const data = JSON.parse(event.data);
				if (data.type !== "Results") return;

				const transcript = data.channel?.alternatives?.[0]?.transcript;
				const isFinal = data.is_final;

				if (transcript && isFinal && transcript.trim()) {
					console.log("Final:", transcript);
					setCaptions((c) => [...c.slice(-20), `You: ${transcript}`]);

					const response = await callGemini(transcript);
					if (response) {
						console.log("AI:", response);
						setCaptions((c) => [...c.slice(-20), `Ivory: ${response}`]);
						speakResponse(response);
					}
				}
			} catch (err) {
				console.warn("Deepgram parse error:", err);
			}
		};

		ws.onerror = () => {
			console.error("Deepgram WS error");
			Alert.alert("Connection failed", "Check Deepgram key or network.");
			handleStop();
		};

		ws.onclose = () => {
			console.log("Deepgram closed");
			wsRef.current = null;
		};
	}, [startRecording]);

	const handleStartListening = () => {
		if (listeningMode) return;

		console.log("Starting listening mode...");
		circleBounce.value = withSpring(
			-15,
			{ damping: 10, stiffness: 200 },
			() => {
				circleBounce.value = withSpring(0, { damping: 12, stiffness: 150 });
			}
		);

		setListeningMode(true);
		setCaptions([]);
		setPaused(false);
		startDeepgram();
	};

	const handlePause = async () => {
		const newPaused = !paused;
		setPaused(newPaused);

		if (recordingRef.current) {
			if (newPaused) {
				await recordingRef.current.pauseAsync();
			} else {
				await recordingRef.current.startAsync();
			}
		}
	};

	const handleStop = async () => {
		console.log("Stopping...");

		if (chunkTimer.current) {
			clearInterval(chunkTimer.current);
			chunkTimer.current = null;
		}

		if (recordingRef.current) {
			try {
				await recordingRef.current.stopAndUnloadAsync();
			} catch (e) {
				console.error("Stop recording error:", e);
			}
			recordingRef.current = null;
		}

		if (wsRef.current) {
			wsRef.current.close();
			wsRef.current = null;
		}

		Speech.stop();

		setListeningMode(false);
		setPaused(false);
		setCaptionsEnabled(false);
		setCaptions([]);

		circleBounce.value = withSpring(10, { damping: 10, stiffness: 200 }, () => {
			circleBounce.value = withSpring(0, { damping: 12, stiffness: 150 });
		});
	};

	const handleToggleCaptions = () => setCaptionsEnabled((c) => !c);

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

	// ── Render ───────────────────────────────────────────────────────────────
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

			{/* New Chat Header */}
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

			{/* Captions Icon */}
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

			{/* Center Content */}
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

			{/* Pills */}
			<PillsControls
				show={listeningMode}
				paused={paused}
				onPause={handlePause}
				onStop={handleStop}
				onShare={() => console.log("Share")}
				onCamera={() => console.log("Camera")}
			/>

			{/* Live captions */}
			<CaptionsList
				visible={listeningMode && captionsEnabled && !paused}
				captions={captions}
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
		width: 120
	},
	newChatHeader: {
		position: "absolute",
		alignSelf: "center",
		zIndex: 9
	},
	newChatText: {
		fontSize: 18,
		fontFamily: "RedRose_500Medium"
	},
	captionsIcon: {
		position: "absolute",
		right: 22,
		zIndex: 999
	},
	centerContent: {
		flex: 1,
		justifyContent: "center",
		alignItems: "center"
	},
	shadowContainer: {
		marginBottom: 44,
		shadowColor: "#4285f4",
		shadowOffset: { width: 5, height: 20 },
		shadowOpacity: 1,
		shadowRadius: 16,
		elevation: 40,
		borderRadius: 140,
		zIndex: 200,
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
		zIndex: 2
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
		borderColor: "#e63946"
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
		top: 5
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
		flex: 1
	},
	flatListContent: {
		paddingVertical: 20,
		paddingHorizontal: 20
	},
	captionText: {
		fontSize: 25,
		textAlign: "auto",
		fontFamily: "RedRose_400Regular",
		lineHeight: 32,
	},
});
