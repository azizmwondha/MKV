# MKV
A small collection of tools for creating and analysing Markov chains.

## Usage
MKV runs in both interactive, and batch mode.

##### Interactive mode

``` java -jar MKV.jar ```

##### Batch mode

``` java -jar MKV.jar batch-file ```

In batch mode, MKV reads all input from a batch file containing MKV instructions.
These are run in sequence from beginning to end. All output is sent to stdout.

Interactive mode prompts the user for commands in command line fashion and 
executes each instruction as it is entered.

## MKV instructions

##### parser
Select a parser for MKV input

``` 
parser word
```

The `word` parser will treat input as text and interpret each space delimited word as a separate state.
This is the default parser.

``` 
parser byte
```

The `byte` parser treats input as a collection of bytes where each byte is a discrete state.

##### scan
Load input data into the Markov model using the selected parser.

``` scan some input in plain text ```

Use the text provided as input 

``` scan file-path ```

Read input data from the provided file. The file path can either be relative or absolute.

``` scan URL ```

Download data from the provided URL and use the response as input.

##### info

```  ```


##### eval

```  ```


##### help


```  ```

##### 