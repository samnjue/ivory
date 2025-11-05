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
	TextInput,
	Keyboard,
} from "react-native";
import { LinearGradient } from "expo-linear-gradient";
import { COLORS } from "../constants/colors";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { FontAwesome6 } from "@expo/vector-icons";
import {
	Menu,
	MoreVertical,
	Paperclip,
	Video,
	X,
	RectangleHorizontal,
	ArrowUp,
	Captions,
	CaptionsOff,
	AudioLines,
	SendHorizontal,
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
						<Stop offset="0%" stopColor="#e63946" stopOpacity="0.8" />
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

// MAIN CHATSCREEN COMPONENT
export default function ChatScreen({ navigation }: { navigation: any }) {
	const colorScheme = useColorScheme();
	const isDark = colorScheme === "dark";
	const colors = isDark ? COLORS.dark : COLORS.light;
	const insets = useSafeAreaInsets();

	// UI States
	const [listeningMode, setListeningMode] = useState(false);
	const [captionsEnabled, setCaptionsEnabled] = useState(false);
	const [paused, setPaused] = useState(false);
	const [inputText, setInputText] = useState("");
	const [inputHeight, setInputHeight] = useState(40);
	const [keyboardVisible, setKeyboardVisible] = useState(false);

	// Animated values
	const menuOpacity = useSharedValue(1);
	const ellipsisOpacity = useSharedValue(1);
	const captionsIconOpacity = useSharedValue(0);
	const chatContentOpacity = useSharedValue(1);
	const inputBarTranslateY = useSharedValue(0);
	const circleOpacity = useSharedValue(0);
	const pauseTextOpacity = useSharedValue(0);
	const circleY = useSharedValue(0);
	const circleScale = useSharedValue(1);
	const circleBounce = useSharedValue(0);
	const typingProgress = useSharedValue(0);
	const sendScale = useSharedValue(0);

	// Keyboard handling
	useEffect(() => {
		const keyboardDidShowListener = Keyboard.addListener(
			"keyboardDidShow",
			() => {
				setKeyboardVisible(true);
				inputBarTranslateY.value = withSpring(-15, {
					damping: 20,
					stiffness: 100,
				});
			}
		);
		const keyboardDidHideListener = Keyboard.addListener(
			"keyboardDidHide",
			() => {
				setKeyboardVisible(false);
				inputBarTranslateY.value = withSpring(0, {
					damping: 20,
					stiffness: 100,
				});
			}
		);

		return () => {
			keyboardDidShowListener.remove();
			keyboardDidHideListener.remove();
		};
	}, []);

	useEffect(() => {
		menuOpacity.value = withTiming(listeningMode ? 0 : 1, { duration: 400 });
		ellipsisOpacity.value = withTiming(listeningMode ? 0 : 1, {
			duration: 400,
		});
		captionsIconOpacity.value = withTiming(listeningMode ? 1 : 0, {
			duration: 420,
		});
		chatContentOpacity.value = withTiming(listeningMode ? 0 : 1, {
			duration: 400,
		});
		inputBarTranslateY.value = withSpring(
			listeningMode ? height : keyboardVisible ? -15 : 0,
			{
				damping: 30,
				stiffness: 90,
			}
		);
		circleOpacity.value = withTiming(listeningMode ? 1 : 0, { duration: 400 });
	}, [listeningMode, keyboardVisible]);

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

	useEffect(() => {
		typingProgress.value = withTiming(inputText.length > 0 ? 1 : 0, {
			duration: 200,
		});
		sendScale.value = withSpring(inputText.length > 0 ? 1 : 0, {
			damping: 20,
			stiffness: 80,
		});
	}, [inputText]);

	// Animated styles
	const menuAnimStyle = useAnimatedStyle(() => ({
		opacity: menuOpacity.value,
	}));
	const ellipsisAnimStyle = useAnimatedStyle(() => ({
		opacity: ellipsisOpacity.value,
	}));
	const captionsIconAnimStyle = useAnimatedStyle(() => ({
		opacity: captionsIconOpacity.value,
	}));
	const chatContentAnimStyle = useAnimatedStyle(() => ({
		opacity: chatContentOpacity.value,
	}));
	const inputBarAnimStyle = useAnimatedStyle(() => ({
		transform: [{ translateY: inputBarTranslateY.value }],
	}));
	const circleAnimStyle = useAnimatedStyle(() => ({
		opacity: circleOpacity.value,
		transform: [
			{ scale: circleScale.value },
			{ translateY: circleY.value + circleBounce.value },
		],
	}));
	const pauseTextAnimStyle = useAnimatedStyle(() => ({
		opacity: pauseTextOpacity.value,
	}));
	const voiceAnimStyle = useAnimatedStyle(() => ({
		opacity: 1 - typingProgress.value,
	}));
	const sendAnimStyle = useAnimatedStyle(() => ({
		opacity: typingProgress.value,
		transform: [{ scale: sendScale.value }],
	}));

	// Captions data (mock)
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

	const handleSend = () => {
		console.log("Sending:", inputText);
		setInputText("");
	};

	return (
		<View style={[styles.container, { backgroundColor: colors.background }]}>
			<View
				style={[
					styles.header,
					{ top: insets.top, backgroundColor: colors.background },
				]}
			>
				<Animated.View style={[styles.headerIconLeft, menuAnimStyle]}>
					<TouchableOpacity onPress={() => navigation.openDrawer()}>
						<Menu size={24} color={colors.text} />
					</TouchableOpacity>
				</Animated.View>
				<Text
					style={[styles.headerTitle, { color: colors.text }]}
					numberOfLines={1}
					ellipsizeMode="tail"
				>
					Planning my weekend trip
				</Text>
				<Animated.View style={[styles.headerIconRight, ellipsisAnimStyle]}>
					<TouchableOpacity>
						<MoreVertical size={24} color={colors.text} />
					</TouchableOpacity>
				</Animated.View>
				<Animated.View
					style={[
						styles.captionsIcon,
						captionsIconAnimStyle,
						{ top: 10, right: 22 },
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
			</View>

			<Animated.View style={[styles.chatContainer, chatContentAnimStyle]}>
				<ScrollView
					style={styles.chatScroll}
					contentContainerStyle={styles.chatContent}
					showsVerticalScrollIndicator={false}
				>
					<View style={styles.userMessageContainer}>
						<View
							style={[
								styles.userMessageBubble,
								{ backgroundColor: isDark ? "#282828" : "#fdfdfdff" },
							]}
						>
							<Text style={[styles.userMessageText, { color: colors.text }]}>
								Hey, I need help planning a weekend trip for this Saturday and
								Sunday. I’m in Nairobi, Kenya, and want to go somewhere nearby,
								maybe 2-3 hours away. I’m into nature and hiking, and I’d prefer
								something budget-friendly. Any ideas?
							</Text>
						</View>
					</View>

					<View style={styles.aiMessageContainer}>
						<Text style={[styles.aiMessageText, { color: colors.text }]}>
							Awesome, a nature-filled weekend sounds perfect! Based on your
							location in Nairobi and preference for hiking, here are three
							budget-friendly destinations within 2-3 hours:{"\n"}
							1. Ngong Hills (~1 hour drive): A scenic range with rolling hills,
							great for a day hike. Entry is about KES 200-600 per person. You
							can enjoy panoramic views and a picnic.{"\n"}
							2. Mount Longonot (~1.5 hours drive): A volcanic crater with a
							rewarding hike. Park fees are ~KES 300 for citizens or ~$26 for
							non-residents. The crater rim trail takes 4-6 hours.{"\n"}
							3. Kereita Forest (~1 hour drive): Offers hiking, ziplining, and
							biking. Entry is ~KES 200, with activities like ziplining at ~KES
							2,000. It’s lush and great for nature lovers.{"\n"}
							Would you like me to focus on one of these, or do you have a
							specific vibe in mind (e.g., solo, with friends, or romantic)?
							Also, do you have a car, or are you using public transport?
						</Text>
					</View>

					<View style={styles.userMessageContainer}>
						<View
							style={[
								styles.userMessageBubble,
								{ backgroundColor: isDark ? "#282828" : "#fdfdfdff" },
							]}
						>
							<Text style={[styles.userMessageText, { color: colors.text }]}>
								Ngong Hills sounds good! I’m going with two friends, and we have
								a car. Can you suggest a hiking plan and what we should bring?
								Also, I have a photo of my hiking gear—can you check if it’s
								suitable?
							</Text>
						</View>
					</View>
				</ScrollView>
			</Animated.View>

			<Animated.View
				style={[
					styles.inputBar,
					inputBarAnimStyle,
					{
						bottom: insets.bottom + 10,
						backgroundColor: isDark ? "#151515" : "#FFFFFF",
					},
				]}
			>
				<TouchableOpacity style={[styles.inputButton]}>
					<Paperclip size={24} color={colors.text} />
				</TouchableOpacity>
				<TextInput
					style={[
						styles.textInput,
						{ color: colors.text, height: Math.max(40, inputHeight) },
					]}
					placeholder="Ask anything"
					placeholderTextColor={colors.text}
					multiline
					onChangeText={setInputText}
					value={inputText}
					onContentSizeChange={(e) =>
						setInputHeight(e.nativeEvent.contentSize.height)
					}
					scrollEnabled
				/>
				<View
					style={{ position: "relative", width: 43, height: 43, marginLeft: 8 }}
				>
					<Animated.View
						style={[{ position: "absolute", left: 0, top: 0 }, voiceAnimStyle]}
						pointerEvents={inputText.length > 0 ? "none" : "auto"}
					>
						<TouchableOpacity onPress={handleStartListening}>
							<LinearGradient
								colors={["#e63946", "#4285f4"]}
								start={{ x: 0, y: 0 }}
								end={{ x: 1, y: 1 }}
								style={styles.miniGradientBorder}
							>
								<View
									style={[
										styles.miniLogoInnerContainer,
										{ backgroundColor: isDark ? "#151515" : "#FFFFFF" },
									]}
								>
									<Image
										source={require("../../assets/ivorystar.png")}
										style={styles.miniLogo}
									/>
								</View>
							</LinearGradient>
						</TouchableOpacity>
					</Animated.View>
					<Animated.View
						style={[
							{
								position: "absolute",
								left: 5,
								top: 9,
								justifyContent: "center",
								alignItems: "center",
							},
							sendAnimStyle,
						]}
						pointerEvents={inputText.length === 0 ? "none" : "auto"}
					>
						<TouchableOpacity onPress={handleSend}>
							<SendHorizontal size={26} color={colors.text} />
						</TouchableOpacity>
					</Animated.View>
				</View>
			</Animated.View>

			<Animated.View
				style={[
					styles.shadowContainer,
					circleAnimStyle,
					{
						position: "absolute",
						alignSelf: "center",
						top: height / 2 - 122.5,
					},
				]}
			>
				<LinearGradient
					colors={["#e63946", "#4285f4"]}
					start={{ x: 0, y: 0 }}
					end={{ x: 1, y: 1 }}
					style={styles.gradientBorder}
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

			<PillsControls
				show={listeningMode}
				paused={paused}
				onPause={handlePause}
				onStop={handleStop}
				onShare={handleShare}
				onCamera={handleCamera}
			/>

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
	header: {
		position: "absolute",
		left: 0,
		right: 0,
		height: 50,
		flexDirection: "row",
		alignItems: "center",
		justifyContent: "center",
		zIndex: 10,
		paddingHorizontal: 16,
	},
	headerIconLeft: {
		position: "absolute",
		left: 16,
	},
	headerTitle: {
		fontSize: 18,
		fontFamily: "RedRose_500Medium",
		maxWidth: width - 120,
		textAlign: "center",
	},
	headerIconRight: {
		position: "absolute",
		right: 16,
	},
	captionsIcon: {
		position: "absolute",
		zIndex: 999,
	},
	chatContainer: {
		flex: 1,
		marginTop: 50 + 25,
	},
	chatScroll: {
		flex: 1,
	},
	chatContent: {
		padding: 16,
		paddingBottom: 80,
	},
	userMessageContainer: {
		alignItems: "flex-end",
		marginBottom: 16,
	},
	userMessageBubble: {
		borderTopLeftRadius: 20,
		borderTopRightRadius: 20,
		borderBottomLeftRadius: 20,
		borderBottomRightRadius: 5,
		padding: 12,
		maxWidth: "80%",
	},
	userMessageText: {
		fontSize: 16,
		fontFamily: "RedRose_400Regular",
	},
	aiMessageContainer: {
		alignItems: "flex-start",
		marginBottom: 16,
		width: "95%",
	},
	aiMessageText: {
		fontSize: 16,
		fontFamily: "RedRose_400Regular",
	},
	inputBar: {
		position: "absolute",
		left: 16,
		right: 16,
		flexDirection: "row",
		alignItems: "flex-end",
		borderRadius: 30,
		padding: 8,
		zIndex: 20,
		elevation: 20,
		shadowColor: "#000000",
		borderWidth: 0.07,
		borderColor: "#7a7a7aff",
	},
	inputButton: {
		width: 40,
		height: 40,
		borderRadius: 20,
		justifyContent: "center",
		alignItems: "center",
		backgroundColor: "transparent",
		bottom: 2,
	},
	textInput: {
		flex: 1,
		fontSize: 16,
		paddingHorizontal: 12,
		paddingVertical: 0,
		maxHeight: 200,
		bottom: 0,
		fontFamily: "RedRose_400Regular",
	},
	voiceButton: {
		marginLeft: 8,
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
		paddingVertical: 12,
		paddingHorizontal: 20,
		borderRadius: 28,
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
