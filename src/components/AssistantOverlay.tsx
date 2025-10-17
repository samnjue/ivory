import React, { useState, useEffect } from "react";
import {
	View,
	Text,
	StyleSheet,
	Image,
	TouchableOpacity,
	Dimensions,
	useColorScheme,
	TextInput,
	Modal,
	Platform,
} from "react-native";
import { BlurView } from "expo-blur";
import { LinearGradient } from "expo-linear-gradient";
import { COLORS } from "../constants/colors";
import { Paperclip, SendHorizontal } from "lucide-react-native";
import Animated, {
	useSharedValue,
	useAnimatedStyle,
	withSpring,
	withTiming,
} from "react-native-reanimated";

const { width, height } = Dimensions.get("window");

interface AssistantOverlayProps {
	visible: boolean;
	onClose: () => void;
	onNavigateToChat?: () => void;
}

export default function AssistantOverlay({
	visible,
	onClose,
	onNavigateToChat,
}: AssistantOverlayProps) {
	const colorScheme = useColorScheme();
	const isDark = colorScheme === "dark";
	const colors = isDark ? COLORS.dark : COLORS.light;

	const [inputText, setInputText] = useState("");
	const [inputHeight, setInputHeight] = useState(40);

	const overlayOpacity = useSharedValue(0);
	const inputBarTranslateY = useSharedValue(100);
	const sendScale = useSharedValue(0);
	const typingProgress = useSharedValue(0);

	useEffect(() => {
		if (visible) {
			overlayOpacity.value = withTiming(1, { duration: 300 });
			inputBarTranslateY.value = withSpring(0, {
				damping: 20,
				stiffness: 100,
			});
		} else {
			overlayOpacity.value = withTiming(0, { duration: 200 });
			inputBarTranslateY.value = withTiming(100, { duration: 200 });
		}
	}, [visible]);

	useEffect(() => {
		typingProgress.value = withTiming(inputText.length > 0 ? 1 : 0, {
			duration: 200,
		});
		sendScale.value = withSpring(inputText.length > 0 ? 1 : 0, {
			damping: 20,
			stiffness: 80,
		});
	}, [inputText]);

	const overlayAnimStyle = useAnimatedStyle(() => ({
		opacity: overlayOpacity.value,
	}));

	const inputBarAnimStyle = useAnimatedStyle(() => ({
		transform: [{ translateY: inputBarTranslateY.value }],
	}));

	const voiceAnimStyle = useAnimatedStyle(() => ({
		opacity: 1 - typingProgress.value,
	}));

	const sendAnimStyle = useAnimatedStyle(() => ({
		opacity: typingProgress.value,
		transform: [{ scale: sendScale.value }],
	}));

	const handleStartListening = () => {
		if (onNavigateToChat) {
			onNavigateToChat();
			onClose();
		}
	};

	const handleSend = () => {
		if (inputText.trim()) {
			console.log("Sending:", inputText);
			// Navigate to chat with the query
			if (onNavigateToChat) {
				onNavigateToChat();
			}
			setInputText("");
			onClose();
		}
	};

	const handleBackdropPress = () => {
		onClose();
	};

	return (
		<Modal
			visible={visible}
			transparent
			animationType="none"
			statusBarTranslucent
			onRequestClose={onClose}
		>
			<Animated.View style={[styles.overlay, overlayAnimStyle]}>
				<TouchableOpacity
					style={styles.backdrop}
					activeOpacity={1}
					onPress={handleBackdropPress}
				/>

				<Animated.View style={[styles.inputBarContainer, inputBarAnimStyle]}>
					<BlurView
						intensity={isDark ? 80 : 60}
						tint={isDark ? "dark" : "light"}
						style={styles.blurContainer}
					>
						<View
							style={[
								styles.inputBar,
								{
									backgroundColor: isDark
										? "rgba(21, 21, 21, 0.7)"
										: "rgba(255, 255, 255, 0.7)",
									borderColor: isDark
										? "rgba(122, 122, 122, 0.3)"
										: "rgba(122, 122, 122, 0.2)",
								},
							]}
						>
							<TouchableOpacity style={styles.inputButton}>
								<Paperclip size={24} color={colors.text} />
							</TouchableOpacity>

							<TextInput
								style={[
									styles.textInput,
									{
										color: colors.text,
										height: Math.max(40, inputHeight),
									},
								]}
								placeholder="Ask anything"
								placeholderTextColor={isDark ? "#999999" : "#666666"}
								multiline
								onChangeText={setInputText}
								value={inputText}
								onContentSizeChange={(e) =>
									setInputHeight(e.nativeEvent.contentSize.height)
								}
								scrollEnabled
								autoFocus
								onSubmitEditing={handleSend}
							/>

							<View style={styles.actionButtonContainer}>
								<Animated.View
									style={[styles.voiceButtonWrapper, voiceAnimStyle]}
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
													{
														backgroundColor: isDark
															? "rgba(21, 21, 21, 0.9)"
															: "rgba(255, 255, 255, 0.9)",
													},
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
									style={[styles.sendButtonWrapper, sendAnimStyle]}
									pointerEvents={inputText.length === 0 ? "none" : "auto"}
								>
									<TouchableOpacity onPress={handleSend}>
										<SendHorizontal size={26} color={colors.text} />
									</TouchableOpacity>
								</Animated.View>
							</View>
						</View>
					</BlurView>
				</Animated.View>
			</Animated.View>
		</Modal>
	);
}

const styles = StyleSheet.create({
	overlay: {
		flex: 1,
		justifyContent: "flex-end",
		backgroundColor: "rgba(0, 0, 0, 0.3)",
	},
	backdrop: {
		...StyleSheet.absoluteFillObject,
	},
	inputBarContainer: {
		marginBottom: 20,
		marginHorizontal: 16,
	},
	blurContainer: {
		borderRadius: 30,
		overflow: "hidden",
	},
	inputBar: {
		flexDirection: "row",
		alignItems: "flex-end",
		borderRadius: 30,
		padding: 8,
		borderWidth: 1,
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
		fontFamily: "RedRose_400Regular",
	},
	actionButtonContainer: {
		position: "relative",
		width: 43,
		height: 43,
		marginLeft: 8,
	},
	voiceButtonWrapper: {
		position: "absolute",
		left: 0,
		top: 0,
	},
	sendButtonWrapper: {
		position: "absolute",
		left: 5,
		top: 9,
		justifyContent: "center",
		alignItems: "center",
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
});