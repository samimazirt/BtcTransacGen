package com.BitcoinTransacGen;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;
import java.net.HttpURLConnection;

import wf.bitcoin.javabitcoindrpcclient.*;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient.ExtendedTxInput;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient.MultiSig;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient.RawTransactionSigningOrVerificationError;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient.SignedRawTransaction;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient.TxInput;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient.Unspent;
import wf.bitcoin.javabitcoindrpcclient.util.Chain;
import wf.bitcoin.javabitcoindrpcclient.util.Util;

interface test extends BitcoindRpcClient {
	Map<String, Object> createWallet(String walletName) throws GenericRpcException;
}

class JsonRPCClient extends BitcoinJSONRPCClient {
	public Map<String, Object> createWallet(String name) throws GenericRpcException {
		return (Map<String, Object>) this.query("createwallet", name, false, false, "", false, false);
	}

	public Map<String, Object> loadWallet(String name) throws GenericRpcException {
		return (Map<String, Object>) this.query("loadwallet", name);
	}

	public Map<String, Object> unloadWallet(String name) throws GenericRpcException {
		return (Map<String, Object>) this.query("unloadwallet", name);
	}
}

public class Application
{
	static final Logger LOGGER = Logger.getLogger(Application.class.getName());

	public static void main(String[] args) throws Exception
	{
		System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT] [%4$-7s] %5$s %n");

		BitcoindRpcClient client = new BitcoinJSONRPCClient();
		Util.ensureRunningOnChain(Chain.REGTEST, client);

		// Before you run the examples:
		// 1. make sure you have an empty regtest chain (e.g. delete the regtest folder in the bitcoin data path)
		// 2. make sure the bitcoin client is running
		// 3. make sure it is running on regtest

		signRawTransactionWithKeyTest_P2SH_MultiSig(client);
		signRawTransactionWithKeyTest_P2SH_P2WPKH(client);
	}

	static void signRawTransactionWithKeyTest_P2SH_MultiSig(BitcoindRpcClient client)
	{
		LOGGER.info("=== Testing scenario: signRawTransactionWithKey ( P2SH-multiSigAddr(2-of-2, [addr1, addr2]) -> addr4 )");

		///////////////////////////////////////////
		// Prepare transaction 1 (addr3 -> multisig)
		///////////////////////////////////////////
		LOGGER.info("Preparing tx1 (addr3 -> multisig)");

		JsonRPCClient jsonRpcClient = new JsonRPCClient();
		String randomWalletName = generateRandomString(10);
		// Call createWallet function from JsonRPCClient
		try {
			Map<String, Object> result = jsonRpcClient.createWallet(randomWalletName);
			String walletName = (String) result.get("name");
			String warning = (String) result.get("warning");
			LOGGER.info("Wallet created: " + walletName);
			if (warning != null) {
				LOGGER.warning("Warning: " + warning);
			}
		} catch (GenericRpcException e) {
			LOGGER.severe("Error creating wallet: " + e.getMessage());
			//return; // Exit the function if an error occurs

			//loading because couldn't create cause already exist
			try {
				Map<String, Object> result = jsonRpcClient.loadWallet(randomWalletName);
				String walletName = (String) result.get("name");
				String warning = (String) result.get("warning");
				LOGGER.info("Wallet loaded: " + walletName);
				if (warning != null) {
					LOGGER.warning("Warning: " + warning);
				}
			} catch (GenericRpcException eLoad) {
				LOGGER.severe("Error loading wallet: " + eLoad.getMessage());
				//return; // Exit the function if an error occurs
			}



		}


		String addr1 = client.getNewAddress();
		LOGGER.info("Created address addr1: " + addr1);

		String addr2 = client.getNewAddress();
		LOGGER.info("Created address addr2: " + addr2);

		// Create P2SH multisig address that wallet can sign
		// Command also adds it to wallet, allowing us to track and spend payments received by that address
		// See https://bitcoin.stackexchange.com/questions/36053/difference-between-createmultisig-and-addmultisigaddress
		MultiSig p2shMultiSig = client.addMultiSigAddress(2, Arrays.asList(addr1, addr2));
		String p2shMultiSigAddr = p2shMultiSig.address();
		LOGGER.info("Created and added to wallet the P2SH-multiSigAddr(2-of-2, [addr1, addr2]) : " + p2shMultiSigAddr);

		String addr3 = client.getNewAddress();
		LOGGER.info("Created address addr3: " + addr3);

		List<String> generatedBlocksHashes = client.generateToAddress(110, addr3);
		LOGGER.info("Generated " + generatedBlocksHashes.size() + " blocks for addr3");

		List<Unspent> availableUtxosForTx1 = client.listUnspent(0, Integer.MAX_VALUE, addr3);
		LOGGER.info("Found " + availableUtxosForTx1.size() + " UTXOs (unspent transaction outputs) belonging to addr3");

		TxInput selectedUtxoForTx1 = availableUtxosForTx1.get(0);
		LOGGER.info("Selected UTXO which will be used in tx1 (addr3 -> P2SH-multiSigAddr) : " + selectedUtxoForTx1);

		//set fee ?
		BigDecimal estimatedFee = BigDecimal.valueOf(0.0000200);
		client.setTxFee(estimatedFee);


		// Fire off transaction 1 (addr3 -> multisig)
		String tx1ID = client.sendToAddress(p2shMultiSigAddr, selectedUtxoForTx1.amount());
		LOGGER.info("UTXO sent to P2SH-multiSigAddr, tx1 ID: " + tx1ID);

		///////////////////////////////////////////
		// Prepare transaction 2 (multisig -> addr4)
		///////////////////////////////////////////
		LOGGER.info("Preparing tx2 (multisig -> addr4)");

		String addr4 = client.getNewAddress();
		LOGGER.info("Created address addr4: " + addr4);

		List<Unspent> availableUtxosForTx2 = client.listUnspent(0, 999, p2shMultiSigAddr);
		LOGGER.info("Found " + availableUtxosForTx2.size() + " UTXOs (unspent transaction outputs) belonging to P2SH-multiSigAddr");

		Unspent selectedUtxoForTx2 = availableUtxosForTx2.get(0);
		LOGGER.info("Selected UTXO which will be used in tx2 (P2SH-multiSigAddr -> addr4) : " + selectedUtxoForTx2);

		ExtendedTxInput inputP2SH = new ExtendedTxInput(
				selectedUtxoForTx2.txid(),
				selectedUtxoForTx2.vout(),
				selectedUtxoForTx2.scriptPubKey(),
				selectedUtxoForTx2.amount(),
				selectedUtxoForTx2.redeemScript(),
				selectedUtxoForTx2.witnessScript());

		LOGGER.info("inputP2SH txid: " + 			inputP2SH.txid());
		LOGGER.info("inputP2SH vout: " + 			inputP2SH.vout());
		LOGGER.info("inputP2SH scriptPubKey: " + 	inputP2SH.scriptPubKey());
		LOGGER.info("inputP2SH amount: " + 			inputP2SH.amount());

		BitcoinRawTxBuilder rawTxBuilder = new BitcoinRawTxBuilder(client);
		rawTxBuilder.in(inputP2SH);

		// Found no other reliable way to estimate the fee in a test
		// Therefore, setting the fee for this tx 200 satoshis (what appears to be the min relay fee)

		BigDecimal txToAddr4Amount = selectedUtxoForTx2.amount().subtract(estimatedFee);
		rawTxBuilder.out(addr4, txToAddr4Amount);

		String unsignedRawMultiSigToAddr4TxHex = rawTxBuilder.create();
		LOGGER.info("Created unsignedRawTx from P2SH-multiSigAddr(2-of-2, [addr1, addr2]) to addr4: " + unsignedRawMultiSigToAddr4TxHex);

		// Sign multi-sig transaction
		SignedRawTransaction srTx = client.signRawTransactionWithKey(
				unsignedRawMultiSigToAddr4TxHex,
				Arrays.asList(client.dumpPrivKey(addr1), client.dumpPrivKey(addr2)), // Using private keys of addr1 and addr2 (multisig 2-of-2)
				Arrays.asList(inputP2SH),
				null);
		LOGGER.info("signedRawTx hex: " + srTx.hex());
		LOGGER.info("signedRawTx complete: " + srTx.complete());

		List<RawTransactionSigningOrVerificationError> errors = srTx.errors();
		if (errors != null)
		{
			LOGGER.severe("Found errors when signing");

			for (RawTransactionSigningOrVerificationError error : errors)
			{
				LOGGER.severe("Error: " + error);
			}
		}

		// Transaction 2 : multisig -> addr4
		String sentRawTransactionID = client.sendRawTransaction(srTx.hex());
		LOGGER.info("Sent signedRawTx (txID): " + sentRawTransactionID);

		BitcoindRpcClient.Transaction transaction = client.getTransaction(sentRawTransactionID);
		System.out.println("Transaction details:\n" + prettyPrintJson(transaction.toString()));

		//unload wallet
		try {
			Map<String, Object> result = jsonRpcClient.unloadWallet(randomWalletName);
			String walletName = (String) result.get("name");
			String warning = (String) result.get("warning");
			LOGGER.info("Wallet unloaded: " + walletName);
			if (warning != null) {
				LOGGER.warning("Warning: " + warning);
			}
		} catch (GenericRpcException e) {
			LOGGER.severe("Error unloading wallet: " + e.getMessage());
			//return; // Exit the function if an error occurs
		}


	}

	/**
	 * Signing a transaction to a P2SH-P2WPKH address (Pay-to-Witness-Public-Key-Hash)
	 */
	static void signRawTransactionWithKeyTest_P2SH_P2WPKH(BitcoindRpcClient client)
	{
		LOGGER.info("=== Testing scenario: signRawTransactionWithKey (addr1 -> addr2)");
		// Call createWallet function from JsonRPCClient
		JsonRPCClient jsonRpcClient = new JsonRPCClient();
		String randomWalletName = generateRandomString(10);

		try {
			Map<String, Object> result = jsonRpcClient.createWallet(randomWalletName);
			String walletName = (String) result.get("name");
			String warning = (String) result.get("warning");
			LOGGER.info("Wallet created: " + walletName);
			if (warning != null) {
				LOGGER.warning("Warning: " + warning);
			}
		} catch (GenericRpcException e) {
			LOGGER.severe("Error creating wallet: " + e.getMessage());
			//return; // Exit the function if an error occurs


			try {
				Map<String, Object> result = jsonRpcClient.loadWallet(randomWalletName);
				String walletName = (String) result.get("name");
				String warning = (String) result.get("warning");
				LOGGER.info("Wallet loaded: " + walletName);
				if (warning != null) {
					LOGGER.warning("Warning: " + warning);
				}
			} catch (GenericRpcException eLoad) {
				LOGGER.severe("Error loading wallet: " + eLoad.getMessage());
				//return; // Exit the function if an error occurs
			}



		}

		String addr1 = client.getNewAddress();
		LOGGER.info("Created address addr1: " + addr1);

		String addr2 = client.getNewAddress();
		LOGGER.info("Created address addr2: " + addr2);

		List<String> generatedBlocksHashes = client.generateToAddress(110, addr1);
		LOGGER.info("Generated " + generatedBlocksHashes.size() + " blocks for addr1");

		List<Unspent> utxos = client.listUnspent(0, Integer.MAX_VALUE, addr1);
		LOGGER.info("Found " + utxos.size() + " UTXOs (unspent transaction outputs) belonging to addr1");

		Unspent selectedUtxo = utxos.get(0);
		LOGGER.info("Selected UTXO which will be sent from addr1 to addr2: " + selectedUtxo);
		//set fee ?
		BigDecimal estimatedFee = BigDecimal.valueOf(0.0000200);
		client.setTxFee(estimatedFee);
		ExtendedTxInput inputP2SH_P2WPKH = new ExtendedTxInput(
				selectedUtxo.txid(),
				selectedUtxo.vout(),
				selectedUtxo.scriptPubKey(),
				selectedUtxo.amount(),
				selectedUtxo.redeemScript(),
				selectedUtxo.witnessScript());
		LOGGER.info("inputP2SH_P2WPKH txid: " + 			inputP2SH_P2WPKH.txid());
		LOGGER.info("inputP2SH_P2WPKH vout: " + 			inputP2SH_P2WPKH.vout());
		LOGGER.info("inputP2SH_P2WPKH scriptPubKey: " + 	inputP2SH_P2WPKH.scriptPubKey());
		LOGGER.info("inputP2SH_P2WPKH redeemScript: " + 	inputP2SH_P2WPKH.redeemScript());
		LOGGER.info("inputP2SH_P2WPKH witnessScript: " + 	inputP2SH_P2WPKH.witnessScript());
		LOGGER.info("inputP2SH_P2WPKH amount: " + 			inputP2SH_P2WPKH.amount());

		BitcoinRawTxBuilder rawTxBuilder = new BitcoinRawTxBuilder(client);
		rawTxBuilder.in(inputP2SH_P2WPKH);

		String tx1ID = client.sendToAddress(addr1, selectedUtxo.amount());
		LOGGER.info("UTXO sent to P2SH-multiSigAddr, tx1 ID: " + tx1ID);
		//System.out.println(transaction.());


		// Found no other reliable way to estimate the fee in a test
		// Therefore, setting the fee for this tx 200 satoshis (what appears to be the min relay fee)
		//BigDecimal estimatedFee = BigDecimal.valueOf(0.00000200);
		BigDecimal txToAddr2Amount = selectedUtxo.amount().subtract(estimatedFee);
		rawTxBuilder.out(addr2, txToAddr2Amount);

		LOGGER.info("unsignedRawTx in amount: " + selectedUtxo.amount());
		LOGGER.info("unsignedRawTx out amount: " + txToAddr2Amount);

		String unsignedRawTxHex = rawTxBuilder.create();

		LOGGER.info("Created unsignedRawTx from addr1 to addr2: " + unsignedRawTxHex);
		// Sign tx
		SignedRawTransaction srTx = client.signRawTransactionWithKey(
				unsignedRawTxHex,
				Arrays.asList(client.dumpPrivKey(addr1)), // addr1 is sending, so we need to sign with the private key of addr1
				Arrays.asList(inputP2SH_P2WPKH),
				null);
		LOGGER.info("signedRawTx hex: " + srTx.hex());
		LOGGER.info("signedRawTx complete: " + srTx.complete());

		List<RawTransactionSigningOrVerificationError> errors = srTx.errors();
		if (errors != null)
		{
			LOGGER.severe("Found errors when signing");

			for (RawTransactionSigningOrVerificationError error : errors)
			{
				LOGGER.severe("Error: " + error);
			}
		}

		String sentRawTransactionID = client.sendRawTransaction(srTx.hex());
		LOGGER.info("Sent signedRawTx (txID): " + sentRawTransactionID);

		BitcoindRpcClient.Transaction transaction = client.getTransaction(sentRawTransactionID);
		//System.out.println(transaction.());
		System.out.println("Transaction details:\n" + prettyPrintJson(transaction.toString()));

		//unload wallet
		try {
			Map<String, Object> result = jsonRpcClient.unloadWallet(randomWalletName);
			String walletName = (String) result.get("name");
			String warning = (String) result.get("warning");
			LOGGER.info("Wallet unloaded: " + walletName);
			if (warning != null) {
				LOGGER.warning("Warning: " + warning);
			}
		} catch (GenericRpcException e) {
			LOGGER.severe("Error unloading wallet: " + e.getMessage());
			//return; // Exit the function if an error occurs
		}
	}

	private static String prettyPrintJson(String jsonData) {
		StringBuilder prettyJson = new StringBuilder();
		int indentLevel = 0;
		boolean inQuotes = false;

		for (char charValue : jsonData.toCharArray()) {
			switch (charValue) {
				case '{':
				case '[':
					prettyJson.append(charValue);
					prettyJson.append('\n');
					indentLevel++;
					addIndentation(indentLevel, prettyJson);
					break;
				case '}':
				case ']':
					prettyJson.append('\n');
					indentLevel--;
					addIndentation(indentLevel, prettyJson);
					prettyJson.append(charValue);
					break;
				case ',':
					prettyJson.append(charValue);
					if (!inQuotes) {
						prettyJson.append('\n');
						addIndentation(indentLevel, prettyJson);
					}
					break;
				case '"':
					prettyJson.append(charValue);
					inQuotes = !inQuotes;
					break;
				default:
					prettyJson.append(charValue);
			}
		}
		return prettyJson.toString();
	}

	public static String generateRandomString(int length) {
		// Define the characters that can be used in the random string
		String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

		// Initialize a Random object
		Random random = new Random();

		// Initialize a StringBuilder to build the random string
		StringBuilder sb = new StringBuilder(length);

		// Loop to generate random characters and append them to the StringBuilder
		for (int i = 0; i < length; i++) {
			// Generate a random index within the range of characters
			int randomIndex = random.nextInt(characters.length());

			// Append the character at the random index to the StringBuilder
			sb.append(characters.charAt(randomIndex));
		}

		// Convert the StringBuilder to a String and return it
		return sb.toString();
	}

	private static void addIndentation(int indentLevel, StringBuilder stringBuilder) {
		for (int i = 0; i < indentLevel; i++) {
			stringBuilder.append("    "); // 4 spaces for each level of indentation
		}
	}

}