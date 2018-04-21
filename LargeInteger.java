/**
 * @author Jonathan Chang
 */

import java.util.Random;
import java.math.BigInteger;
import java.io.Serializable;

public class LargeInteger implements Serializable{
	
	private final byte[] ONE = {(byte) 1};
	private final byte[] ZERO = {(byte) 0};

	private byte[] val;

	/**
	 * Construct the LargeInteger from a given byte array
	 * @param b the byte array that this LargeInteger should represent
	 */
	public LargeInteger(byte[] b) {
		val = b;
	}

	/**
	 * Construct the LargeInteger by generating a random n-bit number that is
	 * probably prime (2^-100 chance of being composite).
	 * @param n the bitlength of the requested integer
	 * @param rnd instance of java.util.Random to use in prime generation
	 */
	public LargeInteger(int n, Random rnd) {
		val = BigInteger.probablePrime(n, rnd).toByteArray();
	}
	
	/**
	 * Return this LargeInteger's val
	 * @return val
	 */
	public byte[] getVal() {
		return val;
	}

	/**
	 * Return the number of bytes in val
	 * @return length of the val byte array
	 */
	public int length() {
		return val.length;
	}

	/** 
	 * Add a new byte as the most significant in this
	 * @param extension the byte to place as most significant
	 */
	public void extend(byte extension) {
		byte[] newv = new byte[val.length + 1];
		newv[0] = extension;
		for (int i = 0; i < val.length; i++) {
			newv[i + 1] = val[i];
		}
		val = newv;
	}

	/**
	 * If this is negative, most significant bit will be 1 meaning most 
	 * significant byte will be a negative signed number
	 * @return true if this is negative, false if positive
	 */
	public boolean isNegative() {
		return (val[0] < 0);
	}

	/**
	 * Computes the sum of this and other
	 * @param other the other LargeInteger to sum with this
	 */
	public LargeInteger add(LargeInteger other) {
		byte[] a, b;
		// If operands are of different sizes, put larger first ...
		if (val.length < other.length()) {
			a = other.getVal();
			b = val;
		}
		else {
			a = val;
			b = other.getVal();
		}

		// ... and normalize size for convenience
		if (b.length < a.length) {
			int diff = a.length - b.length;

			byte pad = (byte) 0;
			if (b[0] < 0) {
				pad = (byte) 0xFF;
			}

			byte[] newb = new byte[a.length];
			for (int i = 0; i < diff; i++) {
				newb[i] = pad;
			}

			for (int i = 0; i < b.length; i++) {
				newb[i + diff] = b[i];
			}

			b = newb;
		}

		// Actually compute the add
		int carry = 0;
		byte[] res = new byte[a.length];
		for (int i = a.length - 1; i >= 0; i--) {
			// Be sure to bitmask so that cast of negative bytes does not
			//  introduce spurious 1 bits into result of cast
			carry = ((int) a[i] & 0xFF) + ((int) b[i] & 0xFF) + carry;

			// Assign to next byte
			res[i] = (byte) (carry & 0xFF);

			// Carry remainder over to next byte (always want to shift in 0s)
			carry = carry >>> 8;
		}

		LargeInteger res_li = new LargeInteger(res);
	
		// If both operands are positive, magnitude could increase as a result
		//  of addition
		if (!this.isNegative() && !other.isNegative()) {
			// If we have either a leftover carry value or we used the last
			//  bit in the most significant byte, we need to extend the result
			if (res_li.isNegative()) {
				res_li.extend((byte) carry);
			}
		}
		// Magnitude could also increase if both operands are negative
		else if (this.isNegative() && other.isNegative()) {
			if (!res_li.isNegative()) {
				res_li.extend((byte) 0xFF);
			}
		}

		// Note that result will always be the same size as biggest input
		//  (e.g., -127 + 128 will use 2 bytes to store the result value 1)
		return res_li;
	}

	/**
	 * Negate val using two's complement representation
	 * @return negation of this
	 */
	public LargeInteger negate() {
		byte[] neg = new byte[val.length];
		int offset = 0;

		// Check to ensure we can represent negation in same length
		//  (e.g., -128 can be represented in 8 bits using two's 
		//  complement, +128 requires 9)
		if (val[0] == (byte) 0x80) { // 0x80 is 10000000
			boolean needs_ex = true;
			for (int i = 1; i < val.length; i++) {
				if (val[i] != (byte) 0) {
					needs_ex = false;
					break;
				}
			}
			// if first byte is 0x80 and all others are 0, must extend
			if (needs_ex) {
				neg = new byte[val.length + 1];
				neg[0] = (byte) 0;
				offset = 1;
			}
		}

		// flip all bits
		for (int i  = 0; i < val.length; i++) {
			neg[i + offset] = (byte) ~val[i];
		}

		LargeInteger neg_li = new LargeInteger(neg);
	
		// add 1 to complete two's complement negation
		return neg_li.add(new LargeInteger(ONE));
	}

	/**
	 * Implement subtraction as simply negation and addition
	 * @param other LargeInteger to subtract from this
	 * @return difference of this and other
	 */
	public LargeInteger subtract(LargeInteger other) {
		return this.add(other.negate());
	}

	/**
	 * Compute the product of this and other
	 * @param other LargeInteger to multiply by this
	 * @return product of this and other
	 */
	public LargeInteger multiply(LargeInteger other) {
		if(this.isZero() || other.isZero()){
			return new LargeInteger(ZERO);
		}

		LargeInteger multiplicand = new LargeInteger(copyArray(val));
		LargeInteger multiplier = new LargeInteger(copyArray(other.getVal()));
		LargeInteger product = new LargeInteger(ZERO);

		while(!multiplier.isZero()){
			byte[] data = multiplier.getVal();
			byte lsb = (byte) (data[data.length-1] & 0x1);// 0000 0001

			if(lsb!=0){//add the curr multiplicand
				LargeInteger temp = new LargeInteger(multiplicand.getVal());
				product=product.add(temp);
			}
			multiplicand = multiplicand.shiftLeft();
			multiplier = multiplier.shiftRight();
		}
		return product;
	}
	
	/**
	 * Run the extended Euclidean algorithm on this and other
	 * @param other another LargeInteger
	 * @return an array structured as follows:
	 *   0:  the GCD of this and other
	 *   1:  a valid x value
	 *   2:  a valid y value
	 * such that this * x + other * y == GCD in index 0
	 */
	 public LargeInteger[] XGCD(LargeInteger other) {
	 	LargeInteger x = new LargeInteger(ZERO);//0
	 	LargeInteger y = new LargeInteger(ONE); //1

	 	LargeInteger last_x = new LargeInteger(ONE); //1
	 	LargeInteger last_y = new LargeInteger(ZERO); //0

	 	LargeInteger a = this;
	 	LargeInteger b = other;

	 	LargeInteger temp;

	 	while(!b.isZero() && !b.isNegative()){
	 		LargeInteger q = a.divide(b);
	 		LargeInteger r = a.mod(b);

	 		temp = x;
	 		x = last_x.subtract((q.multiply(x)));
	 		last_x = temp;


			temp = y;
	 		y = last_y.subtract((q.multiply(y)));
	 		last_y = temp;

	 		a=b;
	 		b=r;
		}

		return new LargeInteger[]{a, last_x, last_y};

	 }

	 /**
	  * Compute the result of raising this to the power of y mod n
	  * @param y exponent to raise this to
	  * @param n modulus value to use
	  * @return this^y mod n
	  */
	 public LargeInteger modularExp(LargeInteger y, LargeInteger n) { //2^k arr method
	 	if(y.isZero()){
	 		return this.mod(n);
		}
		 LargeInteger result = new LargeInteger (ONE); //Start with result = 1
		 LargeInteger base = this;
		 LargeInteger powCopy = y;

		 base = base.mod(n);

		 byte[] temp;
		 byte lsb;

		while(!powCopy.isZero()){
		 	temp = powCopy.getVal();
			 lsb = (byte) (temp[temp.length-1] & 0x1);
			 if(lsb!=0){ //ODD
			 	LargeInteger oddB = base.mod(n);
			 	LargeInteger oddR = result.mod(n);
				 result = oddR.multiply(oddB).mod(n);
			 }
			 LargeInteger evenB = base.mod(n);
			 base = evenB.multiply(evenB).mod(n);
			 powCopy = powCopy.shiftRight();
		 }

		 return result;
	 }

	public LargeInteger divide(LargeInteger other){
		if(other.isOne()){
			return this;
		}
		if(other.isZero()){
			return null;
		}
		//otherwise there is a quotient
		LargeInteger dividend = new LargeInteger(copyArray(this.getVal()));
		LargeInteger quotient = new LargeInteger(ZERO);
		LargeInteger one = new LargeInteger(ONE);

		byte[] checker = copyArray(other.getVal());
		int lenB = checker.length;

		int lenA = dividend.length();
		if(dividend.getVal()[0]==0){
			lenA--;
		}
		int totalLen = lenA+lenB;

		byte[] shifted = new byte[totalLen]; //puts all 0's at end
		for(int i =0;i<lenB;i++){
			shifted[i] = checker[i];
		}

		LargeInteger divisor = new LargeInteger(shifted);

		int check = 8*dividend.length();
		if(dividend.getVal()[0]==0){
			check-=8;
		}
		for(int i=0;i<check;i++){
			divisor = divisor.shiftRight();
			quotient = quotient.shiftLeft();
			int sub = dividend.compare(divisor);
			if(sub !=-1 && !dividend.isZero()){
				dividend = dividend.subtract(divisor);
				quotient = quotient.add(one);
			}
		}
		return quotient;
	}

	 private LargeInteger mod(LargeInteger other){
	 	LargeInteger zero = new LargeInteger(ZERO);
		//a mod b = a - b[a/b]
		 if(other.isOne()){
		 	return zero;
		 }
		 int compare = this.compare(other);
		 if(compare==0){
		 	return zero;
		 }

		LargeInteger divResult = this.divide(other);
		LargeInteger mulResult = divResult.multiply(other);
		return this.subtract(mulResult);
	}

	private boolean isZero(){
		for(int i = 0; i < val.length; i++){ //Make sure every byte has the value 0
			if(val[i] != 0) {
				return false;
			}
		}
		return true;
	}

	private static byte[] copyArray(byte[] arr){
		byte[] output = new byte[arr.length];
		for(int i = 0; i < arr.length; i++){
			output[i] = arr[i];
		}
		return output;
	}

	private LargeInteger shiftLeft(){
	 	if(this.isZero()){
	 		return this;
		}
		//byte[] dataCopy = copyArray(val);
		boolean carry = false;
		byte msb;
		for(int j = val.length-1; j >= 0; j--){ //Shift every bit to the left by 1
			msb = (byte) (val[j] & 0x80); //if msb is 1 transfer to next byte
			val[j] <<= 1; //Shift left by 1
			if(carry){
				val[j] |= 0x1; //set lsb is 1, from prev byte
				carry=false;
			}
			if(msb != 0){
				carry = true;
			}
		}
		msb = (byte) (val[0] & 0x80);
		if(msb!=0){ //MSB overall was a 1, expand
			byte[] output = new byte[val.length+1];
			for(int j = 0; j < val.length; j++){
				output[j+1] = val[j];
			}
			val = output;
		}
		return new LargeInteger(val);
	}

	private LargeInteger shiftRight(){
		if(this.isZero()){
			return this;
		}
		//byte[] dataCopy = copyArray(val);
		boolean carry = false;
		for(int j = 0; j < val.length; j++){
			byte lsb = (byte) (val[j] & 0x1);
			val[j] &= 0xff;
			val[j] >>=1; //Shift right
			val[j] &=0x7f;
			if(carry) {
				val[j] |= 0x80; //set MSB to 1, from prev byte
				carry=false;
			}
			if(lsb != 0){
				carry = true;
			}
		}
		return new LargeInteger(val);
	}

	public int compare(LargeInteger other) {
		byte[] thisV = this.getVal();
		byte[] otherV = other.getVal();

		int startA=0, startB=0;
		if(!this.isZero()){
			for(int i=0;thisV[i]==0;i++){
				startA++;
			}
		}

		if(!other.isZero()){
			for(int i=0;otherV[i]==0;i++){
				startB++;
			}
		}
		//make sure only MSB are looked at
		if(startA!=0){
			thisV = delLeadZero(thisV, startA);
		}
		if(startB!=0){
			otherV = delLeadZero(otherV, startB);
		}

		//use length (diff size) to return
				if(thisV.length > otherV.length){
					return 1;
				}
				if(otherV.length > thisV.length){
					return -1;
				}

		//if same sign and same length, check bit by bit
		return checkBits(thisV, otherV);
	}

	private static byte[] delLeadZero(byte[] arr, int start){
		byte[] output = new byte[arr.length-start];
		for(int i = 0; i < output.length; i++){
			output[i] = arr[i+start];
		}
		return output;
	}

	private int checkBits(byte[] a, byte[] b){ //compare two arrays of the same length
		for (int i = 0; i < a.length; i++) { //scan bytes
			byte currBitVal = (byte) 128;
			for (int j = 7; j >= 0; j--) { //Scan bits
				byte bit1, bit2;
				if((a[i] & currBitVal) == 0){
					bit1=0;
				}
				else{
					bit1=1;
				}

				if((b[i] & currBitVal) == 0){
					bit2=0;
				}
				else{
					bit2=1;
				}

				if (bit1 > bit2) {
					return 1;
				}
				if (bit1 < bit2) {
					return -1;
				}
				currBitVal &= 0xff;
				currBitVal >>=1; //Shift right
				currBitVal &= 0x7f;
			}
		}
		return 0;
	}

	private boolean isOne(){
		if(val[val.length-1] != 1){
			return false;
		}
		for(int i = 0; i < val.length-1; i++){
			if(val[i] != 0){
				return false;
			}
		}
		return true;
	}
}
