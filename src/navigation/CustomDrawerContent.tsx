import React from "react";
import {
	View,
	Text,
	StyleSheet,
	Image,
	TouchableOpacity,
	TextInput,
	useColorScheme,
	ScrollView,
} from "react-native";
import { Feather } from "@expo/vector-icons";
import { LinearGradient } from "expo-linear-gradient";
import MaskedView from "@react-native-masked-view/masked-view";
import { COLORS } from "../constants/colors";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { supabase } from "../utils/supabase";

const recentChats = [
	{
		day: "Tuesday",
		chats: [
			"Planning my weekend trip",
			"Voice notes for my project",
			"Organizing study schedule",
		],
	},
	{
		day: "Monday",
		chats: ["Workout plan for beginners", "Finding a nearby coffee shop"],
	},
	{
		day: "Last Week",
		chats: [
			"Moving to Nairobi guide",
			"Compatibilist Account of Free Will",
			"Easy dinner recipes",
		],
	},
	{
		day: "August",
		chats: ["Weird animal facts", "Voice journal for the day"],
	},
];

export default function CustomDrawerContent({ navigation }: any) {
	const colorScheme = useColorScheme();
	const isDark = colorScheme === "dark";
	const colors = isDark ? COLORS.dark : COLORS.light;
	const insets = useSafeAreaInsets();

	const avatarSource = require("../../assets/20250805_160609.jpg");
	const logoSource = require("../../assets/ivorystar.png");

	return (
		<View
			style={[
				styles.container,
				{ backgroundColor: colors.background, paddingTop: insets.top },
			]}
		>
			{/* Search bar */}
			<View style={styles.searchContainer}>
				<View
					style={[
						styles.searchWrapper,
						{ backgroundColor: isDark ? "#3a3a3aff" : "#FFFFFF" },
					]}
				>
					<Feather
						name="search"
						size={18}
						color={isDark ? "#999" : "#666"}
						style={styles.searchIcon}
					/>
					<TextInput
						style={[styles.searchInput, { color: colors.text }]}
						placeholder="Search"
						placeholderTextColor={isDark ? "#999" : "#666"}
					/>
				</View>
			</View>

			{/* New Chat Button with Gradient Text */}
			<TouchableOpacity
				style={styles.newChatBtn}
				onPress={() => {
					/* handle new chat */
				}}
			>
				<View style={styles.starIcon}>
					<Image source={logoSource} style={styles.logoMini} />
				</View>
				<MaskedView
					maskElement={<Text style={styles.newChatTxt}>New Chat</Text>}
				>
					<LinearGradient
						colors={["#e63946", "#4285f4"]}
						start={{ x: 0, y: 0 }}
						end={{ x: 1, y: 0 }}
						style={styles.gradientContainer}
					>
						<Text style={[styles.newChatTxt, { opacity: 0 }]}>New Chat</Text>
					</LinearGradient>
				</MaskedView>
			</TouchableOpacity>

			{/* Chat List */}
			<ScrollView style={styles.chatList} showsVerticalScrollIndicator={false}>
				<Text style={[styles.sectionTitle, { color: colors.text }]}>
					Recent Chats
				</Text>
				{recentChats.map((section) => (
					<View key={section.day}>
						<Text style={[styles.chatDay, { color: colors.text }]}>
							{section.day}
						</Text>
						{section.chats.map((chat) => (
							<TouchableOpacity
								key={chat}
								style={styles.chatItem}
								onPress={() => navigation.navigate("ChatScreen")}
							>
								<Text
									style={[styles.chatText, { color: colors.text }]}
									numberOfLines={1}
								>
									{chat}
								</Text>
							</TouchableOpacity>
						))}
					</View>
				))}
			</ScrollView>

			{/* Footer Profile Section */}
			<View
				style={[
					styles.footer,
					{
						borderTopColor: isDark ? "#333" : "#e0e0e0",
						paddingBottom: insets.bottom + 12,
					},
				]}
			>
				<TouchableOpacity onPress={() => navigation.navigate("SettingsScreen")}>
					<View style={styles.profileRow}>
						<Image source={avatarSource} style={styles.avatar} />
						<View style={styles.profileInfo}>
							<Text style={[styles.profileName, { color: colors.text }]}>
								Sammy Njue
							</Text>
							<Text
								style={[
									styles.profileEmail,
									{ color: isDark ? "#999" : "#666" },
								]}
							>
								sammynjue10@gmail.com
							</Text>
						</View>
						<TouchableOpacity
							style={styles.menuButton}
							onPress={() => navigation.navigate("SettingsScreen")}
						>
							<Text style={[styles.menuDots, { color: colors.text }]}>⋮</Text>
						</TouchableOpacity>
					</View>
				</TouchableOpacity>
			</View>
		</View>
	);
}

const styles = StyleSheet.create({
	container: {
		flex: 1,
		paddingHorizontal: 16,
	},
	searchContainer: {
		width: "100%",
		marginTop: 16,
		marginBottom: 16,
	},
	searchWrapper: {
		flexDirection: "row",
		alignItems: "center",
		borderRadius: 35,
		paddingHorizontal: 14,
		paddingVertical: 0,
	},
	searchIcon: {
		marginRight: 8,
	},
	searchInput: {
		flex: 1,
		fontSize: 15,
		fontFamily: "RedRose_400Regular",
		top: 2,
	},
	newChatBtn: {
		flexDirection: "row",
		alignItems: "center",
		marginBottom: 4,
		paddingVertical: 4,
	},
	starIcon: {
		marginRight: 10,
	},
	logoMini: {
		width: 32,
		height: 32,
		top: 4,
	},
	gradientContainer: {
		paddingVertical: 2,
	},
	newChatTxt: {
		fontSize: 20,
		fontFamily: "RedRose_600SemiBold",
		top: 8,
	},
	chatList: {
		flex: 1,
	},
	sectionTitle: {
		fontSize: 18,
		fontFamily: "RedRose_600SemiBold",
		marginBottom: 0,
		top: 8,
	},
	chatDay: {
		fontSize: 14,
		fontFamily: "RedRose_600SemiBold",
		marginTop: 16,
		marginBottom: 8,
	},
	chatItem: {
		paddingVertical: 8,
		paddingLeft: 4,
	},
	chatText: {
		fontSize: 15,
		fontFamily: "RedRose_400Regular",
	},
	footer: {
		borderTopWidth: 1,
		paddingTop: 16,
		paddingBottom: 12,
	},
	profileRow: {
		flexDirection: "row",
		alignItems: "center",
	},
	avatar: {
		width: 40,
		height: 40,
		borderRadius: 20,
		backgroundColor: "#ddd",
	},
	profileInfo: {
		flex: 1,
		marginLeft: 12,
	},
	profileName: {
		fontSize: 15,
		fontFamily: "RedRose_600SemiBold",
		marginBottom: 2,
	},
	profileEmail: {
		fontSize: 13,
		fontFamily: "RedRose_400Regular",
	},
	menuButton: {
		padding: 8,
	},
	menuDots: {
		fontSize: 20,
		fontWeight: "bold",
	},
});
