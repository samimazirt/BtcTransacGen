package com.BitcoinTransacGen;

import java.io.Reader;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import wf.bitcoin.javabitcoindrpcclient.*;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient.ExtendedTxInput;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient.RawTransactionSigningOrVerificationError;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient.SignedRawTransaction;
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

		//signRawTransactionWithKeyTest_P2SH_MultiSig(client);
		signRawTransactionWithKeyTest_P2SH_P2WPKH(client);
	}



	/**
	 * Signing a transaction to a P2SH-P2WPKH address (Pay-to-Witness-Public-Key-Hash)
	 *
	 * @return
	 */
	static String signRawTransactionWithKeyTest_P2SH_P2WPKH(BitcoindRpcClient client)
	{
		LOGGER.info("=== Testing scenario: signRawTransactionWithKey (addr1 -> addr2)");
		// Call createWallet function from JsonRPCClient
		JsonRPCClient jsonRpcClient = new JsonRPCClient();
		String randomWalletName = generateRandomString(10);
		BigDecimal minAmount = BigDecimal.valueOf(0.0001); // Minimum amount
		BigDecimal maxAmount = BigDecimal.valueOf(1.0);

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

		List<String> generatedBlocksHashes = client.generateToAddress(510, addr1);
		LOGGER.info("Generated " + generatedBlocksHashes.size() + " blocks for addr1");

		System.out.println("ballll" + client.getBalance());

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
		System.out.println("mmmmm" + client.getTransaction(tx1ID));


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

		BitcoindRpcClient.RawTransaction rawTransactionDecoded = client.getRawTransaction(jsonRpcClient.decodeRawTransaction(srTx.hex()).vIn().get(0).txid());
		String transaction = client.getTransaction(sentRawTransactionID).toString().replaceFirst(addr2.toString(), addr1.toString());
		//System.out.println("transac details"+ prettyPrintJson(transaction));
		System.out.println("ballll" + client.getBalance());
		Gson gson = new Gson();




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

		return transaction;
	}

	public static BigDecimal generateRandomAmount(BigDecimal min, BigDecimal max) {
		Random random = new Random();

		// Define the distribution of transaction amounts
		// For simplicity, you can define a distribution based on ranges and their corresponding probabilities
		// Adjust the probabilities based on your analysis of real-world Bitcoin transaction data
		double[] rangeProbabilities = {0.1, 0.2, 0.3, 0.2, 0.1, 0.05, 0.03, 0.02};

		// Define the ranges corresponding to the probabilities
		BigDecimal[] rangeMinimums = {
				BigDecimal.valueOf(0.0001),
				BigDecimal.valueOf(0.001),
				BigDecimal.valueOf(0.01),
				BigDecimal.valueOf(0.1),
				BigDecimal.valueOf(1.0),
				BigDecimal.valueOf(2.0),
				BigDecimal.valueOf(5.0),
				BigDecimal.valueOf(10.0)
		};

		// Select a range based on probabilities
		double randomNumber = random.nextDouble();
		double cumulativeProbability = 0.0;
		int selectedRangeIndex = 0;
		for (int i = 0; i < rangeProbabilities.length; i++) {
			cumulativeProbability += rangeProbabilities[i];
			if (randomNumber <= cumulativeProbability) {
				selectedRangeIndex = i;
				break;
			}
		}

		// Generate a random value within the selected range
		BigDecimal selectedRangeMin = rangeMinimums[selectedRangeIndex];
		BigDecimal selectedRangeMax = (selectedRangeIndex == rangeMinimums.length - 1) ? max : rangeMinimums[selectedRangeIndex + 1];
		BigDecimal randomBigDecimal = selectedRangeMin.add(new BigDecimal(random.nextDouble()).multiply(selectedRangeMax.subtract(selectedRangeMin)));

		return randomBigDecimal.setScale(8, BigDecimal.ROUND_HALF_UP);
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