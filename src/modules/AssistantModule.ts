import {
	NativeModules,
	NativeEventEmitter,
	Platform,
	Alert,
} from "react-native";

console.log("Available Native Modules:", NativeModules);
console.log("AssistantModule:", NativeModules.AssistantModule);

interface AssistantModuleInterface {
	requestAssistPermission: () => Promise<boolean>;
	isAssistantEnabled: () => Promise<boolean>;
	requestOverlayPermission: () => Promise<boolean>;
	hasOverlayPermission: () => Promise<boolean>;
	showAssistOverlay: () => void;
	finishActivity: () => void;
}

const { AssistantModule } = NativeModules;

if (AssistantModule) {
	console.log("AssistantModule keys:", Object.keys(AssistantModule));
}

class AssistantAPI {
	private eventEmitter: NativeEventEmitter | null = null;

	constructor() {
		if (Platform.OS === "android" && AssistantModule) {
			this.eventEmitter = new NativeEventEmitter(AssistantModule);
		}
	}

	/**
	 * Request permission to become the default assistant
	 * Opens Android settings where user can select your app
	 */
	async requestAssistPermission(): Promise<boolean> {
		if (Platform.OS !== "android" || !AssistantModule) {
			Alert.alert("Assistant Module only available on Android");
			return false;
		}
		try {
			return await AssistantModule.requestAssistPermission();
		} catch (error) {
			console.error("Error requesting assist permission:", error);
			return false;
		}
	}

	/**
	 * Check if the app is set as default assistant
	 */
	async isAssistantEnabled(): Promise<boolean> {
		if (Platform.OS !== "android" || !AssistantModule) {
			return false;
		}
		try {
			return await AssistantModule.isAssistantEnabled();
		} catch (error) {
			console.error("Error checking assistant status:", error);
			return false;
		}
	}

	/**
	 * Request permission to draw overlays on other apps
	 * Required for system-wide overlay functionality
	 */
	async requestOverlayPermission(): Promise<boolean> {
		if (Platform.OS !== "android" || !AssistantModule) {
			Alert.alert("Overlay permission only available on Android");
			return false;
		}
		try {
			return await AssistantModule.requestOverlayPermission();
		} catch (error) {
			console.error("Error requesting overlay permission:", error);
			return false;
		}
	}

	/**
	 * Check if the app has permission to draw overlays
	 */
	async hasOverlayPermission(): Promise<boolean> {
		if (Platform.OS !== "android" || !AssistantModule) {
			return false;
		}
		try {
			return await AssistantModule.hasOverlayPermission();
		} catch (error) {
			console.error("Error checking overlay permission:", error);
			return false;
		}
	}

	/**
	 * Subscribe to assistant trigger events
	 * Returns a cleanup function to remove the listener
	 */
	addAssistListener(callback: () => void): (() => void) | null {
		if (!this.eventEmitter) {
			return null;
		}

		const subscription = this.eventEmitter.addListener(
			"onAssistRequested",
			callback
		);

		return () => subscription.remove();
	}

	/**
	 * Finish the current activity (used in assist mode)
	 */
	finishActivity(): void {
		if (Platform.OS === "android" && AssistantModule) {
			AssistantModule.finishActivity();
		}
	}

	async startFloatingOrb(): Promise<boolean> {
		if (Platform.OS !== "android" || !AssistantModule) return false;
		try {
			return await AssistantModule.startFloatingOrb();
		} catch (e) {
			console.error("Failed to start orb:", e);
			return false;
		}
	}

	async stopFloatingOrb(): Promise<boolean> {
		if (Platform.OS !== "android" || !AssistantModule) return false;
		try {
			return await AssistantModule.stopFloatingOrb();
		} catch (e) {
			console.error("Failed to stop orb:", e);
			return false;
		}
	}

	async isFloatingOrbRunning(): Promise<boolean> {
		if (Platform.OS !== "android" || !AssistantModule) return false;
		return await AssistantModule.isFloatingOrbRunning();
	}
}

export default new AssistantAPI();
