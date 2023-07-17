package com.example.reactnativeandroidfragmentexample.Biometrics;


import android.os.Build;

import android.util.Log;
import java.nio.charset.StandardCharsets;

import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.biometric.BiometricPrompt.AuthenticationCallback;
import androidx.biometric.BiometricPrompt.PromptInfo;
import androidx.fragment.app.FragmentActivity;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.UiThreadUtil;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


import org.bouncycastle.crypto.digests.KeccakDigest;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;


public class Biometrics extends ReactContextBaseJavaModule {
    protected String biometricKeyAlias = "biometric_key";

    public Biometrics(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    String domainSeparator = "EIP712Domain(string name,string version,uint256 chainId,address verifyingContract)";
    String messageTypes = "{\"Person\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"wallet\",\"type\":\"address\"}]}";
    String primaryType = "Person";

    @Override
    public String getName() {
        return "Biometrics";
    }

    private static byte[] createEIP712Message(String domainSeparator, String messageTypes, String primaryType, Map<String, Object> messageData) {
        byte[] domainSeparatorHash = keccak256(domainSeparator.getBytes(StandardCharsets.UTF_8));
        byte[] typesHash = keccak256(messageTypes.getBytes(StandardCharsets.UTF_8));
        byte[] primaryTypeHash = keccak256(primaryType.getBytes(StandardCharsets.UTF_8));
        byte[] messageHash = keccak256(getMessageDataHash(domainSeparatorHash, primaryType, messageData));

        byte[] concatenatedBytes = concatenateByteArrays(domainSeparatorHash, typesHash, primaryTypeHash, messageHash);
        return keccak256(concatenatedBytes);
    }

    private static byte[] getMessageDataHash(byte[] domainSeparatorHash, String primaryType, Map<String, Object> messageData) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append((char) 0x19);
        stringBuilder.append((char) 0x01);
        stringBuilder.append((char) 0x01);
        stringBuilder.append(new String(domainSeparatorHash, StandardCharsets.UTF_8));
        stringBuilder.append(encodeType(primaryType, messageData));
        stringBuilder.append(encodeData(primaryType, messageData));
        return keccak256(stringBuilder.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static String encodeType(String primaryType, Map<String, Object> messageData) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(primaryType);
        stringBuilder.append("(");
        for (String key : messageData.keySet()) {
            stringBuilder.append(key);
            stringBuilder.append(",");
        }
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        stringBuilder.append(")");
        return stringBuilder.toString();
    }

    private static String encodeData(String primaryType, Map<String, Object> messageData) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String key : messageData.keySet()) {
            stringBuilder.append(getTypeData(primaryType, key));
            stringBuilder.append(":");
            stringBuilder.append(messageData.get(key));
            stringBuilder.append(",");
        }
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        return stringBuilder.toString();
    }

    private static String getTypeData(String primaryType, String key) {
        return primaryType + "." + key;
    }

    private static byte[] concatenateByteArrays(byte[]... arrays) {
        int totalLength = Arrays.stream(arrays).mapToInt(array -> array.length).sum();
        byte[] result = new byte[totalLength];
        int currentIndex = 0;
        for (byte[] array : arrays) {
            System.arraycopy(array, 0, result, currentIndex, array.length);
            currentIndex += array.length;
        }
        return result;
    }

    private static byte[] keccak256(byte[] input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("Keccak-256");
            return digest.digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Keccak-256 algorithm not available", e);
        }
    }

    private void sendEvent(String eventName, WritableMap params) {
        ReactApplicationContext context = getReactApplicationContext();
        if (context.hasActiveCatalystInstance()) {
            context.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(eventName, params);
        }
    }

    @ReactMethod
    public void createDupa(Promise promise) {
        // Perform your native code logic here

        // Example code:
        try {
            WritableMap resultMap = new WritableNativeMap();
            resultMap.putBoolean("keysDeleted", true);
            promise.resolve(resultMap);
        } catch (Exception e) {
            // If there's an error, reject the promise with an error message
            promise.reject("ERROR_CODE", "Error message: " + e.getMessage());
        }
    }

    private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);
    public static String bytesToHex(byte[] bytes) {
        byte[] hexChars = new byte[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars, StandardCharsets.UTF_8);
    }

    @ReactMethod
    public Sign.SignatureData createKeys(final ReadableMap params, Promise promise) {

        String messageFromNative = params.getString("message");
        System.out.println("MESSAGE FROM NATIVE: " + messageFromNative);
        try {
            if (isCurrentSDKMarshmallowOrLater()) {
                deleteBiometricKey();

                String privateKeyHex = "10bebaa85ac304f3234dc443e08ab53710a32b4c3e762ce522afef1f5bf8283f";
                String message = "Hello, World!";


                BigInteger privateKeyBigInt = new BigInteger(privateKeyHex, 16);
                ECKeyPair ecKeyPair = ECKeyPair.create(privateKeyBigInt);
                String address = Keys.getAddress(ecKeyPair);
                System.out.println("Address: " + address);


                byte[] messageBytes = message.getBytes();
                KeccakDigest digest = new KeccakDigest(256);
                byte[] hashBytes = new byte[digest.getDigestSize()];
                digest.update(messageBytes, 0, messageBytes.length);
                digest.doFinal(hashBytes, 0);
                System.out.println("hsdh: " + bytesToHex(hashBytes));

                Sign.SignatureData signatureData = Sign.signMessage(hashBytes, ecKeyPair, false);


                String r = Numeric.toHexStringNoPrefix(signatureData.getR());
                String s = Numeric.toHexStringNoPrefix(signatureData.getS());
                String v = Numeric.toHexStringNoPrefix(signatureData.getV());

                WritableMap signatureMap = Arguments.createMap();
                signatureMap.putString("r", r);
                signatureMap.putString("s", s);
                signatureMap.putString("v", v);
                System.out.println("response: " + signatureMap.toString());

                // Resolve the promise with the signature data
                sendEvent("SignatureDataEvent", signatureMap);


//                byte[] messageBytes = message.getBytes();
//                KeccakDigest digest = new KeccakDigest(256);
//                byte[] hashBytes = new byte[digest.getDigestSize()];
//                digest.update(messageBytes, 0, messageBytes.length);
//                digest.doFinal(hashBytes, 0);
//
//                Sign.SignatureData signatureData = Sign.signMessage(hashBytes, ecKeyPair, false);
//
//                String r = Numeric.toHexStringNoPrefix(signatureData.getR());
//                String s = Numeric.toHexStringNoPrefix(signatureData.getS());
//                byte v = signatureData.getV()[0];
//
//                System.out.println("Signature r: " + r);
//                System.out.println("Signature s: " + s);
//                System.out.println("Signature v: " + v);





                return null;

                // try to save it in storage
//                KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
//                keyStore.load(null);
//
//                KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
//                        biometricKeyAlias, KeyProperties.PURPOSE_SIGN)
//                        .setDigests(KeyProperties.DIGEST_SHA256)
//                        .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
//                        .setKeySize(256)
//                        .build();
//
//                KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(
//                        KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore");
//                keyPairGenerator.initialize(spec);
//                keyPairGenerator.generateKeyPair();



            } else {
                promise.reject("Cannot generate keys on android versions below 6.0", "Cannot generate keys on android versions below 6.0");
            }
        } catch (Exception e) {
            Log.d("ERR", String.valueOf(e));
            promise.reject("Error generating public private keys: " + e.getMessage(), "Error generating public private keys");
        }
        return null;
    }

    public KeyPair retrievePrivateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);

        KeyStore.Entry entry = keyStore.getEntry(biometricKeyAlias, null);
        if (entry instanceof KeyStore.PrivateKeyEntry) {
            KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) entry;
            return new KeyPair(privateKeyEntry.getCertificate().getPublicKey(),
                    privateKeyEntry.getPrivateKey());
        }

        throw new RuntimeException("Private key not found in Keystore");
    }

    private boolean isCurrentSDKMarshmallowOrLater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    @ReactMethod
    public void deleteKeys(Promise promise) {
        System.out.println("SASASASASASASASA");
        if (doesBiometricKeyExist()) {
            boolean deletionSuccessful = deleteBiometricKey();

            if (deletionSuccessful) {
                WritableMap resultMap = new WritableNativeMap();
                resultMap.putBoolean("keysDeleted", true);
                promise.resolve(resultMap);
            } else {
                promise.reject("Error deleting biometric key from keystore", "Error deleting biometric key from keystore");
            }
        } else {
            WritableMap resultMap = new WritableNativeMap();
            resultMap.putBoolean("keysDeleted", false);
            promise.resolve(resultMap);
        }
    }

    @ReactMethod
    public void createSignature(final ReadableMap params, final Promise promise) {
        Log.d("createSignature", "Create signature");
        if (isCurrentSDKMarshmallowOrLater()) {
            UiThreadUtil.runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            Log.d("createSignature", "1");
                            try {
                                String promptMessage = params.getString("promptMessage");
                                String payload = params.getString("payload");
                                String cancelButtonText = params.getString("cancelButtonText");
                                boolean allowDeviceCredentials = params.getBoolean("allowDeviceCredentials");



                                KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
                                keyStore.load(null);

                                // Retrieve the private key from the Android Keystore
                                KeyStore.Entry entry = keyStore.getEntry(biometricKeyAlias, null);

                                System.out.println(("AAAAAAAAAAAAAA"));

                                if (entry instanceof KeyStore.PrivateKeyEntry) {
                                    PrivateKey privateKey = ((KeyStore.PrivateKeyEntry) entry).getPrivateKey();
                                    byte[] encodedPrivateKey = privateKey.getEncoded();
                                    // Convert the encoded private key to a hexadecimal string
                                    BigInteger privateKeyBigInt = new BigInteger(1, encodedPrivateKey);
                                    String privateKeyHex = privateKeyBigInt.toString(16);

                                }

//                                ECKeyPair ecKeyPair = ECKeyPair.create(new BigInteger(privateKey, 16));


//                                String str = "hello world";
//                                Sign.SignatureData sigData = Sign.signMessage(str.getBytes(), ecKeyPair);
//
//                                Credentials credentials = Credentials.create(ecKeyPair);
//                                String address = credentials.getAddress();
//
//                                // Print the Ethereum wallet address
//                                System.out.println("Wallet Address: " + address);
//
//                                // Sign a message using the private key
//                                String message = "Hello, Ethereum!";
//                                byte[] messageHash = Hash.sha3(message.getBytes());
//                                Sign.SignatureData signature = Sign.signMessage(messageHash, credentials.getEcKeyPair());
//
//                                // Print the message signature
//                                String r = Numeric.toHexString(signature.getR());
//                                String s = Numeric.toHexString(signature.getS());
//                                byte[] v = signature.getV();
//                                System.out.println("Message: " + message);
//                                System.out.println("Signature (R): " + r);
//                                System.out.println("Signature (S): " + s);
//                                System.out.println("Signature (V): " + v);


//                                Signature signature = Signature.getInstance("SHA256withRSA");
//                                KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
//                                keyStore.load(null);
//                                PrivateKey privateKey = (PrivateKey) keyStore.getKey(biometricKeyAlias, null);
//                                signature.initSign(privateKey);
//                                BiometricPrompt.CryptoObject cryptoObject = new BiometricPrompt.CryptoObject(signature);
//                                AuthenticationCallback authCallback = new CreateSignatureCallback(promise, payload);
//                                FragmentActivity fragmentActivity = (FragmentActivity) getCurrentActivity();
//                                Executor executor = Executors.newSingleThreadExecutor();
//                                BiometricPrompt biometricPrompt = new BiometricPrompt(fragmentActivity, executor, authCallback);
//                                biometricPrompt.authenticate(getPromptInfo(promptMessage, cancelButtonText, allowDeviceCredentials), cryptoObject);

                            } catch (Exception e) {
                                Log.d("createSignature", "ERROR");
                                promise.reject("Error signing payload: " + e.getMessage(), "Error generating signature: " + e.getMessage());
                            }
                        }
                    });
        } else {
            Log.d("createSignature", "ERROR2");
            promise.reject("Cannot generate keys on android versions below 6.0", "Cannot generate keys on android versions below 6.0");
        }
    }



    @ReactMethod
    public void isSensorAvailable(final ReadableMap params, final Promise promise) {
        try {
            if (isCurrentSDKMarshmallowOrLater()) {

                boolean allowDeviceCredentials = params.getBoolean("allowDeviceCredentials");
                ReactApplicationContext reactApplicationContext = getReactApplicationContext();
                BiometricManager biometricManager = BiometricManager.from(reactApplicationContext);
                int canAuthenticate = biometricManager.canAuthenticate(getAllowedAuthenticators(allowDeviceCredentials));

                if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
                    WritableMap resultMap = new WritableNativeMap();
                    resultMap.putBoolean("available", true);
                    resultMap.putString("biometryType", "Biometrics");
                    promise.resolve(resultMap);
                } else {
                    WritableMap resultMap = new WritableNativeMap();
                    resultMap.putBoolean("available", false);

                    switch (canAuthenticate) {
                        case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                            resultMap.putString("error", "BIOMETRIC_ERROR_NO_HARDWARE");
                            break;
                        case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                            resultMap.putString("error", "BIOMETRIC_ERROR_HW_UNAVAILABLE");
                            break;
                        case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                            resultMap.putString("error", "BIOMETRIC_ERROR_NONE_ENROLLED");
                            break;
                        case BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED:
                            resultMap.putString("error", "BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED");
                            break;
                    }

                    promise.resolve(resultMap);
                }
            } else {
                WritableMap resultMap = new WritableNativeMap();
                resultMap.putBoolean("available", false);
                resultMap.putString("error", "Unsupported android version");
                promise.resolve(resultMap);
            }
        } catch (Exception e) {
            promise.reject("Error detecting biometrics availability: " + e.getMessage(), "Error detecting biometrics availability: " + e.getMessage());
        }
    }
    private PromptInfo getPromptInfo(String promptMessage, String cancelButtonText, boolean allowDeviceCredentials) {
        PromptInfo.Builder builder = new PromptInfo.Builder().setTitle(promptMessage);

        builder.setAllowedAuthenticators(getAllowedAuthenticators(allowDeviceCredentials));

        if (allowDeviceCredentials == false || isCurrentSDK29OrEarlier()) {
            builder.setNegativeButtonText(cancelButtonText);
        }

        return builder.build();
    }

    private int getAllowedAuthenticators(boolean allowDeviceCredentials) {
        if (allowDeviceCredentials && !isCurrentSDK29OrEarlier()) {
            return BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL;
        }
        return BiometricManager.Authenticators.BIOMETRIC_STRONG;
    }

    private boolean isCurrentSDK29OrEarlier() {
        return Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q;
    }

    @ReactMethod
    public void simplePrompt(final ReadableMap params, final Promise promise) {
        if (isCurrentSDKMarshmallowOrLater()) {
            UiThreadUtil.runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            try {
                                String promptMessage = params.getString("promptMessage");
                                String cancelButtonText = params.getString("cancelButtonText");
                                boolean allowDeviceCredentials = params.getBoolean("allowDeviceCredentials");

                                AuthenticationCallback authCallback = new SimplePromptCallback(promise);
                                FragmentActivity fragmentActivity = (FragmentActivity) getCurrentActivity();
                                Executor executor = Executors.newSingleThreadExecutor();
                                BiometricPrompt biometricPrompt = new BiometricPrompt(fragmentActivity, executor, authCallback);

                                biometricPrompt.authenticate(getPromptInfo(promptMessage, cancelButtonText, allowDeviceCredentials));
                            } catch (Exception e) {
                                promise.reject("Error displaying local biometric prompt: " + e.getMessage(), "Error displaying local biometric prompt: " + e.getMessage());
                            }
                        }
                    });
        } else {
            promise.reject("Cannot display biometric prompt on android versions below 6.0", "Cannot display biometric prompt on android versions below 6.0");
        }
    }

    @ReactMethod
    public void biometricKeysExist(Promise promise) {
        try {
            boolean doesBiometricKeyExist = doesBiometricKeyExist();
            WritableMap resultMap = new WritableNativeMap();
            resultMap.putBoolean("keysExist", doesBiometricKeyExist);
            promise.resolve(resultMap);
        } catch (Exception e) {
            promise.reject("Error checking if biometric key exists: " + e.getMessage(), "Error checking if biometric key exists: " + e.getMessage());
        }
    }

    protected boolean doesBiometricKeyExist() {
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);

            return keyStore.containsAlias(biometricKeyAlias);
        } catch (Exception e) {
            return false;
        }
    }

    protected boolean deleteBiometricKey() {
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);

            keyStore.deleteEntry(biometricKeyAlias);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

