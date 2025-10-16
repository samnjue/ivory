import React, { useState, useEffect, createContext, useContext } from "react";
import { useColorScheme } from "react-native";
import AsyncStorage from "@react-native-async-storage/async-storage";

type Theme = "light" | "dark" | "system";

interface ThemeContextType {
	theme: "light" | "dark";
	setTheme: (theme: Theme) => void;
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined);

export const ThemeProvider: React.FC<{ children: React.ReactNode }> = ({
	children,
}) => {
	const systemTheme = useColorScheme();
	const [userTheme, setUserTheme] = useState<Theme>("system");

	useEffect(() => {
		AsyncStorage.getItem("userTheme").then((storedTheme) => {
			if (storedTheme) setUserTheme(storedTheme as Theme);
		});
	}, []);

	useEffect(() => {
		if (userTheme === "system") {
			const resolvedTheme = systemTheme ?? "light";
			AsyncStorage.setItem("userTheme", resolvedTheme);
			setUserTheme(resolvedTheme);
		}
	}, [systemTheme, userTheme]);

	const setTheme = (newTheme: Theme) => {
		setUserTheme(newTheme);
		AsyncStorage.setItem("userTheme", newTheme);
	};

	const effectiveTheme =
		userTheme === "system" ? systemTheme ?? "light" : userTheme;

	return (
		<ThemeContext.Provider value={{ theme: effectiveTheme, setTheme }}>
			{children}
		</ThemeContext.Provider>
	);
};

export const useTheme = () => {
	const context = useContext(ThemeContext);
	if (!context) throw new Error("useTheme must be used within ThemeProvider");
	return context;
};
