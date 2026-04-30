# Java AWS S3 Demo

A command-line Java application that demonstrates how to interact with Amazon S3 cloud storage using the AWS SDK for Java v2. Covers the four core storage operations: upload, list, download, and delete.

---

## What It Does

When you run the program it asks for your S3 bucket name, then drops you into a menu that loops until you quit:

```
What would you like to do?
  1. Upload a file
  2. List files
  3. Download a file
  4. Delete a file
  q. Quit
```

- **Upload** — pick any local file (type `.` to browse the current directory), sends it to your bucket
- **List** — prints every file in the bucket with its size
- **Download** — pulls a file from the bucket and saves it locally
- **Delete** — shows the bucket contents, then removes the file you choose

---

## Prerequisites

- Java 11 or higher
- [Apache Maven](https://maven.apache.org/)
- An [AWS account](https://aws.amazon.com/) with an S3 bucket created
- AWS CLI installed and configured

---

## AWS Credential Setup

Run this once before using the program. It stores your keys locally so they never appear in source code:

```bash
aws configure
```

You will be prompted for:
```
AWS Access Key ID:     <your key>
AWS Secret Access Key: <your secret>
Default region:        us-east-1
Default output format: json
```

---

## How to Run

1. Clone or download the project
2. Open a terminal and navigate to the `s3-demo` folder
3. Run:

```bash
mvn compile exec:java
```

4. Enter your bucket name when prompted and use the menu

---

## Project Structure

```
s3-demo/
├── pom.xml                        # Maven build config + AWS SDK dependency
├── sample.txt                     # Example file for testing uploads
└── src/main/java/
    └── S3Demo.java                # Main program
```

---

## Dependencies

- [AWS SDK for Java v2](https://github.com/aws/aws-sdk-java-v2) — `software.amazon.awssdk:s3:2.25.70`
- Managed via the AWS BOM for version alignment
