import { NativeModules, NativeEventEmitter, Platform, Alert } from "react-native";

console.log("Available Native Modules:", NativeModules);
console.log("AssistantModule:", NativeModules.AssistantModule);
interface AssistantModuleInterface {
	requestAssistPermission: () => Promise<boolean>;
	isAssistantEnabled: () => Promise<boolean>;
	showAssistOverlay: () => void;
}

const { AssistantModule } = NativeModules;

class AssistantAPI {
	private eventEmitter: NativeEventEmitter | null = null;

	constructor() {
		if (Platform.OS === "android" && AssistantModule) {
			this.eventEmitter = new NativeEventEmitter(AssistantModule);
		}
	}

	//Request permission to become the default assistant
	//Opens Android settings where user can select your app
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

	//Check if the app is set as default assistant
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

	//Subscribe to assistant trigger events
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
}

export default new AssistantAPI();
