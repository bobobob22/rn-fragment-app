import React, { useEffect, useState } from "react";
import "@ethersproject/shims";
import EncryptedStorage from "react-native-encrypted-storage";

import { Wallet, ethers } from "ethers";

import {
  StyleSheet,
  View,
  Text,
  TouchableOpacity,
  NativeModules,
  DeviceEventEmitter,
} from "react-native";
import Title from "../components/Title";

const domain = {
  name: "My App",
  version: "1",
  chainId: 5,
  verifyingContract: "0x1111111111111111111111111111111111111111",
};

const types = {
  Mail: [
    { name: "from", type: "Person" },
    { name: "to", type: "Person" },
    { name: "content", type: "string" },
  ],
  Person: [
    { name: "name", type: "string" },
    { name: "wallet", type: "address" },
  ],
};

const mail = {
  from: {
    name: "Alice",
    wallet: "0x2111111111111111111111111111111111111111",
  },
  to: {
    name: "Bob",
    wallet: "0x3111111111111111111111111111111111111111",
  },
  content: "Hello!",
};

const wallet_key_storage = "WalletKey";
const mnemonic_key_storage = "MnemonicKey";
const url = "http://10.0.2.2:3001/eth-signer/";

export default function HelloWorld() {
  const [signature, setSignature] = useState("");
  const [signerAddress, setSignerAddress] = useState("");
  const [isValid, setIsValid] = useState(false);

  useEffect(() => {
    DeviceEventEmitter.addListener("customEventName", function (e) {
      // handle event and you will get a value in event object, you can log it here
      console.log(e);
    });
  }, []);

  const handleClick = () => {
    NativeModules.CommunicationModule.sendMessageToNative("MSG FROM RN");
  };

  const removeWallet = async () => {
    try {
      const wallet = await EncryptedStorage.removeItem(wallet_key_storage);
      console.log(
        "removeWallet wallet_key_storagefrom encrypted storage",
        wallet
      );
    } catch (err) {
      console.log("removeWallet err", err);
    }
  };

  const getMnemonic = async () => {
    const mnemonic = await EncryptedStorage.getItem(mnemonic_key_storage);
    return JSON.parse(mnemonic);
  };

  const generateWallet = async () => {
    const wallet = Wallet.createRandom();

    try {
      await EncryptedStorage.setItem(
        wallet_key_storage,
        JSON.stringify({
          wallet,
          privateKey: wallet.privateKey,
        })
      );

      await EncryptedStorage.setItem(
        mnemonic_key_storage,
        JSON.stringify({ mnemonicPhrase: wallet.mnemonic.phrase })
      );

      return {
        privateKey: wallet.privateKey,
        walletAddress: wallet.address,
      };
    } catch (err) {
      console.log("generateWallet error", err);
    }
  };

  const getOrGenerateWallet = async () => {
    const wallet = await EncryptedStorage.getItem(wallet_key_storage);

    if (!wallet) {
      console.log("Generate wallet");
      return await generateWallet();
    }

    return JSON.parse(wallet);
  };

  const signMessage = async () => {
    console.log("signMessage");
    await getOrGenerateWallet();

    const mnemonic = await getMnemonic();
    const fromMnemonic = ethers.Wallet.fromMnemonic(mnemonic.mnemonicPhrase);
    const address = await fromMnemonic.getAddress();

    const signature = await fromMnemonic._signTypedData(domain, types, mail);

    console.log("signMessage signature", signature);

    setSignature(signature);
    setSignerAddress(address);
  };

  const verifyMessage = async () => {
    console.log("verifyMessage");

    const recoveredAddress = ethers.utils.verifyTypedData(
      domain,
      types,
      mail,
      signature
    );
    const isValid = recoveredAddress === signerAddress;
    setIsValid(isValid);
  };

  return (
    <View style={styles.container}>
      <TouchableOpacity
        onPress={handleClick}
        style={{ backgroundColor: "green", marginBottom: 10 }}
      >
        <Title title="Send msg" />
      </TouchableOpacity>

      <TouchableOpacity
        onPress={signMessage}
        style={{ backgroundColor: "black", marginBottom: 10 }}
      >
        <Title title="Sign message " />
      </TouchableOpacity>

      <TouchableOpacity
        onPress={verifyMessage}
        style={{ backgroundColor: "orange" }}
      >
        <Title title="Verify message" />
      </TouchableOpacity>
      <Text
        style={{ color: "#fff", textAlign: "center", margin: 10, fontSize: 16 }}
      >
        {isValid ? "Valid signature" : "No valid"}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: "center",
    backgroundColor: "blue",
  },
});
