import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.stream.Collectors;

public class S3Demo {

    public static void main(String[] args) {

        // ── Connect to S3 ──────────────────────────────────────────────────
        S3Client s3;
        try {
            s3 = S3Client.builder()
                    .region(Region.US_EAST_1)
                    .credentialsProvider(ProfileCredentialsProvider.create())
                    .build();
        } catch (SdkClientException e) {
            System.out.println("[ERROR] Could not connect to AWS.");
            System.out.println("        Make sure you have run 'aws configure' and your credentials are set up.");
            return;
        }

        // ── Shutdown hook: fires on Ctrl+C so the connection closes cleanly ─
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[INFO] Interrupted. Closing AWS connection...");
            s3.close();
        }));

        Scanner scanner = new Scanner(System.in);

        // ── Bucket name input ──────────────────────────────────────────────
        String bucket = "";
        while (bucket.isEmpty()) {
            System.out.print("Enter your S3 bucket name: ");
            try {
                bucket = scanner.nextLine().trim();
            } catch (NoSuchElementException e) {
                System.out.println("\n[INFO] Input stream closed. Exiting.");
                scanner.close();
                s3.close();
                return;
            }
            if (bucket.isEmpty()) {
                System.out.println("[ERROR] Bucket name cannot be empty. Please try again.");
            }
        }

        // ── Main loop ──────────────────────────────────────────────────────
        while (true) {
            System.out.println("\nWhat would you like to do?");
            System.out.println("  1. Upload a file");
            System.out.println("  2. List files");
            System.out.println("  3. Download a file");
            System.out.println("  4. Delete a file");
            System.out.println("  q. Quit");
            System.out.print("Enter choice: ");

            String choice;
            try {
                choice = scanner.nextLine().trim().toLowerCase();
            } catch (NoSuchElementException e) {
                System.out.println("\n[INFO] Input stream closed. Exiting.");
                break;
            }

            switch (choice) {

                // ── Upload ─────────────────────────────────────────────────
                case "1":
                    System.out.print("Enter the path of the file to upload (or . , .. , ../.. to browse a directory): ");
                    try {
                        String uploadPath = scanner.nextLine().trim();
                        if (uploadPath.isEmpty()) {
                            System.out.println("[ERROR] File path cannot be empty.");
                            break;
                        }
                        Path uploadSrc = Path.of(uploadPath);
                        if (Files.isDirectory(uploadSrc)) {
                            uploadPath = browseDirectory(scanner, uploadSrc);
                            if (uploadPath == null) break;
                            uploadSrc = Path.of(uploadPath);
                        }
                        if (!Files.exists(uploadSrc)) {
                            System.out.println("[ERROR] File not found: " + uploadPath);
                            System.out.println("        Check the path and try again.");
                            break;
                        }
                        if (!Files.isReadable(uploadSrc)) {
                            System.out.println("[ERROR] Cannot read file: " + uploadPath);
                            System.out.println("        Check file permissions and try again.");
                            break;
                        }
                        String key = uploadSrc.getFileName().toString();
                        uploadFile(s3, bucket, key, uploadSrc);
                        System.out.println("[OK] Uploaded: " + key);
                        System.out.println("\nObjects in bucket \"" + bucket + "\":");
                        listFiles(s3, bucket);
                    } catch (InvalidPathException e) {
                        System.out.println("[ERROR] Invalid file path: " + e.getReason());
                    } catch (NoSuchBucketException e) {
                        System.out.println("[ERROR] Bucket \"" + bucket + "\" does not exist.");
                        System.out.println("        Create it in the AWS Console or check the bucket name.");
                    } catch (S3Exception e) {
                        System.out.println("[ERROR] Upload failed: " + e.awsErrorDetails().errorMessage());
                    } catch (SdkClientException e) {
                        System.out.println("[ERROR] Connection error. Check your internet connection.");
                    }
                    break;

                // ── List ───────────────────────────────────────────────────
                case "2":
                    try {
                        System.out.println("\nObjects in bucket \"" + bucket + "\":");
                        listFiles(s3, bucket);
                    } catch (NoSuchBucketException e) {
                        System.out.println("[ERROR] Bucket \"" + bucket + "\" does not exist.");
                        System.out.println("        Create it in the AWS Console or check the bucket name.");
                    } catch (S3Exception e) {
                        System.out.println("[ERROR] Could not list files: " + e.awsErrorDetails().errorMessage());
                    } catch (SdkClientException e) {
                        System.out.println("[ERROR] Connection error. Check your internet connection.");
                    }
                    break;

                // ── Download ───────────────────────────────────────────────
                case "3":
                    try {
                        System.out.print("Enter the filename to download: ");
                        String downloadKey = scanner.nextLine().trim();
                        if (downloadKey.isEmpty()) {
                            System.out.println("[ERROR] Filename cannot be empty.");
                            break;
                        }
                        System.out.print("Enter destination path (file, or . , .. , ../.. for a directory): ");
                        String destPath = scanner.nextLine().trim();
                        if (destPath.isEmpty()) {
                            System.out.println("[ERROR] Destination path cannot be empty.");
                            break;
                        }
                        Path dest = Path.of(destPath);
                        if (Files.isDirectory(dest)) {
                            dest = dest.resolve(Path.of(downloadKey).getFileName().toString());
                        }
                        if (Files.exists(dest)) {
                            System.out.println("[ERROR] File already exists: " + dest.toAbsolutePath().normalize());
                            System.out.println("        Choose a different destination or remove the existing file.");
                            break;
                        }
                        downloadFile(s3, bucket, downloadKey, dest);
                        System.out.println("[OK] Downloaded \"" + downloadKey + "\" -> " + dest.toAbsolutePath().normalize());
                    } catch (NoSuchBucketException e) {
                        System.out.println("[ERROR] Bucket \"" + bucket + "\" does not exist.");
                        System.out.println("        Create it in the AWS Console or check the bucket name.");
                    } catch (NoSuchKeyException e) {
                        System.out.println("[ERROR] File not found in bucket.");
                        System.out.println("        Use option 2 to see what files are available.");
                    } catch (InvalidPathException e) {
                        System.out.println("[ERROR] Invalid destination path: " + e.getReason());
                    } catch (S3Exception e) {
                        System.out.println("[ERROR] Download failed: " + e.awsErrorDetails().errorMessage());
                    } catch (SdkClientException e) {
                        System.out.println("[ERROR] Connection error. Check your internet connection.");
                    }
                    break;

                // ── Delete ─────────────────────────────────────────────────
                case "4":
                    try {
                        System.out.println("\nObjects in bucket \"" + bucket + "\":");
                        listFiles(s3, bucket);
                        System.out.print("\nEnter the filename to delete: ");
                        String deleteKey = scanner.nextLine().trim();
                        if (deleteKey.isEmpty()) {
                            System.out.println("[ERROR] Filename cannot be empty.");
                            break;
                        }
                        deleteFile(s3, bucket, deleteKey);
                        System.out.println("[OK] Deleted: " + deleteKey);
                        System.out.println("\nObjects in bucket \"" + bucket + "\":");
                        listFiles(s3, bucket);
                    } catch (NoSuchBucketException e) {
                        System.out.println("[ERROR] Bucket \"" + bucket + "\" does not exist.");
                        System.out.println("        Create it in the AWS Console or check the bucket name.");
                    } catch (S3Exception e) {
                        System.out.println("[ERROR] Delete failed: " + e.awsErrorDetails().errorMessage());
                    } catch (SdkClientException e) {
                        System.out.println("[ERROR] Connection error. Check your internet connection.");
                    }
                    break;

                // ── Quit ───────────────────────────────────────────────────
                case "q":
                    System.out.println("Goodbye.");
                    scanner.close();
                    s3.close();
                    return;

                default:
                    System.out.println("[ERROR] \"" + choice + "\" is not a valid option.");
                    System.out.println("        Enter 1, 2, 3, 4, or q.");
            }
        }

        scanner.close();
        s3.close();
    }

    static String browseDirectory(Scanner scanner, Path dir) {
        try {
            List<Path> files = Files.list(dir)
                    .filter(Files::isRegularFile)
                    .collect(Collectors.toList());

            if (files.isEmpty()) {
                System.out.println("[ERROR] No files found in: " + dir.toAbsolutePath().normalize());
                return null;
            }

            System.out.println("\nFiles in " + dir.toAbsolutePath().normalize() + ":");
            for (int i = 0; i < files.size(); i++) {
                System.out.printf("  %d. %s%n", i + 1, files.get(i).getFileName());
            }
            System.out.print("Enter number: ");

            String input = scanner.nextLine().trim();
            int index;
            try {
                index = Integer.parseInt(input) - 1;
            } catch (NumberFormatException e) {
                System.out.println("[ERROR] Please enter a number from the list.");
                return null;
            }

            if (index < 0 || index >= files.size()) {
                System.out.println("[ERROR] Number out of range. Enter a number between 1 and " + files.size() + ".");
                return null;
            }

            return files.get(index).toString();

        } catch (IOException e) {
            System.out.println("[ERROR] Could not read current directory: " + e.getMessage());
            return null;
        }
    }

    static void uploadFile(S3Client s3, String bucket, String key, Path file) {
        s3.putObject(
            PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build(),
            RequestBody.fromFile(file)
        );
    }

    static void listFiles(S3Client s3, String bucket) {
        ListObjectsV2Response response = s3.listObjectsV2(
            ListObjectsV2Request.builder()
                .bucket(bucket)
                .build()
        );
        if (response.contents().isEmpty()) {
            System.out.println("  (bucket is empty)");
        } else {
            response.contents().forEach(obj ->
                System.out.printf("  %-40s  %,d bytes%n", obj.key(), obj.size())
            );
        }
    }

    static void downloadFile(S3Client s3, String bucket, String key, Path dest) {
        s3.getObject(
            GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build(),
            dest
        );
    }

    static void deleteFile(S3Client s3, String bucket, String key) {
        s3.deleteObject(
            DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build()
        );
    }
}
