import React, { useEffect, useState } from "react";
import "@ethersproject/shims";
import EncryptedStorage from "react-native-encrypted-storage";
// import * as Keychain from 'react-native-keychain';
import base64 from 'base64-js'

import { Wallet, ethers, utils, keccak256, recoverAddress } from "ethers";

import {
  StyleSheet,
  View,
  Text,
  TouchableOpacity,
  NativeModules,
  DeviceEventEmitter,
} from "react-native";
import Title from "../components/Title";

//new
// Define the domain separator
const domainSeparator =
  "EIP712Domain(string name,string version,uint256 chainId,address verifyingContract)";
// Define the message types
const messageTypes =
  '{"Person":[{"name":"name","type":"string"},{"name":"wallet","type":"address"}]}';
// Define the primary type
const primaryType = "Person";
// Define the message data
const messageData = {
  name: "Alice",
  wallet: "0x123456789...",
};

//end new

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

function createEIP712Message(
  domainSeparator,
  messageTypes,
  primaryType,
  messageData
) {
  const domainSeparatorHash = ethers.utils.keccak256(
    ethers.utils.toUtf8Bytes(domainSeparator)
  );
  const typesHash = ethers.utils.keccak256(
    ethers.utils.toUtf8Bytes(messageTypes)
  );
  const primaryTypeHash = ethers.utils.keccak256(
    ethers.utils.toUtf8Bytes(primaryType)
  );
  const messageHash = getMessageDataHash(
    domainSeparatorHash,
    primaryType,
    messageData
  );

  const concatenatedBytes = concatenateByteArrays(
    domainSeparatorHash,
    typesHash,
    primaryTypeHash,
    messageHash
  );
  const result = ethers.utils.keccak256(concatenatedBytes);
  return result;
}

function getMessageDataHash(domainSeparatorHash, primaryType, messageData) {
  const stringBuilder = [
    String.fromCharCode(0x19),
    String.fromCharCode(0x01),
    String.fromCharCode(0x01),
    utils.hexlify(domainSeparatorHash),
    encodeType(primaryType, messageData),
    encodeData(primaryType, messageData),
  ].join("");

  return ethers.utils.keccak256(utils.toUtf8Bytes(stringBuilder));
}

function encodeType(primaryType, messageData) {
  let typeString = `${primaryType}(`;
  for (const key of Object.keys(messageData)) {
    typeString += `${key},`;
  }
  typeString = typeString.slice(0, -1); // Remove trailing comma
  typeString += ")";
  return typeString;
}

function encodeData(primaryType, messageData) {
  let dataString = "";
  for (const key of Object.keys(messageData)) {
    dataString += `${getTypeData(primaryType, key)}:${messageData[key]},`;
  }
  dataString = dataString.slice(0, -1); // Remove trailing comma
  return dataString;
}

function getTypeData(primaryType, key) {
  return `${primaryType}.${key}`;
}

function concatenateByteArrays(...arrays) {
  const concatenatedArray = [];
  for (const array of arrays) {
    concatenatedArray.push(...array);
  }
  return Uint8Array.from(concatenatedArray);
}

export default function HelloWorld() {
  const [signature, setSignature] = useState("");
  const [signerAddress, setSignerAddress] = useState("");
  const [isValid, setIsValid] = useState(false);

  useEffect(() => {
    DeviceEventEmitter.addListener("customEventName", function (e) {
      console.log("customEventName", e);
    });
  }, []);

  const sendSimpleMsgFromRn = () => {
    NativeModules.CommunicationModule.sendMessageToNative("MSG FROM RN");
  };

  const signAndVerifiedMessage = async () => {
    try {
      const signaturePromise = new Promise((resolve) => {
        const eventListener = (signature) => {
          const hash = utils.keccak256(utils.toUtf8Bytes('Hello, World!'));
          const verifiedAddress = utils.recoverAddress(hash, {
            r: `0x${signature.r}`,
            s: `0x${signature.s}`,
            v: `0x${signature.v}`,
          });
  
          console.log('verifiedAddress:', verifiedAddress);
          resolve(verifiedAddress);
        };
  
        DeviceEventEmitter.addListener('SignatureDataEvent', eventListener);
      });

      await NativeModules.Biometrics.createKeys({
        allowDeviceCredentials: true,
        message: "Hello world",
      });

      return await signaturePromise;
    } catch (err) {
      console.log(err)
    }
  }

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
    console.log("!!!", wallet);
    if (wallet) {
      console.log("@@@", wallet.privateKey);
    }

    if (!wallet) {
      console.log("Generate wallet");
      return await generateWallet();
    }

    return JSON.parse(wallet);
  };

  const signMessage = async () => {
    // await removeWallet()
    console.log("signMessage");
    await getOrGenerateWallet();

    const testPrivateKey =
      "0x10bebaa85ac304f3234dc443e08ab53710a32b4c3e762ce522afef1f5bf8283f";

    const walletFromPrivateKey = new ethers.Wallet(testPrivateKey);
    console.log(
      "walletFromPrivateKey address",
      walletFromPrivateKey.getAddress()
    );

    // Create the EIP-712 message
    const message = createEIP712Message(
      domainSeparator,
      messageTypes,
      primaryType,
      messageData
    );

    // Sign the message with a private key
    const signingKey = new ethers.utils.SigningKey(testPrivateKey);
    const signature = signingKey.signDigest(message);

    // Sign the typed data

    // Print the EIP-712 signature
    console.log("EIP-712 Signature:", signature);
    console.log("EIP-712", signature.r.toString(16));
    console.log("EIP-712", signature.s.toString(16));
    console.log("EIP-712", signature.v.toString(16));

   
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
        onPress={sendSimpleMsgFromRn}
        style={{ backgroundColor: "green", marginBottom: 10 }}
      >
        <Title title="Send msg from rn" />
      </TouchableOpacity>

      <TouchableOpacity
        onPress={signAndVerifiedMessage}
        style={{ backgroundColor: "green", marginBottom: 10 }}
      >
        <Title title="sign and verified message" />
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

      {/* <TouchableOpacity
        onPress={testKeyChain}
        style={{ backgroundColor: "orange" }}
      >
        <Title title="Test keychain" />
      </TouchableOpacity> */}
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
