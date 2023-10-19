import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.CSVPrinter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        // Read CSV files into lists
        List<UserRecord> listA = readCSV("File_A.csv");
        List<UserRecord> listB = readCSV("File_B.csv");

        // Print the resulting lists
        System.out.println("List A:");
        printUserList(listA);
        System.out.println("List B:");
        printUserList(listB);

        // Merge the lists
        List<UserRecord> mergedList = mergeLists(listA, listB);

        // Print the merged list
        System.out.println("Merged List:");
        printUserList(mergedList);

        // Make an API request
        String apiResponse = makeApiRequest();

        // Convert the JSON response to a user matrix
        List<UserRecord> userMatrix = jsonToUserMatrix(apiResponse);

        // Update UIDs in the merged list
        updateUidsInMergedList(mergedList, userMatrix);

        // Print the final list
        System.out.println("Final List:");
        printUserList(mergedList);

        // Save the merged and updated list to a new CSV file
        saveToCSV(mergedList, "merged_file.csv");
    }

    // Read CSV file into a list of UserRecord objects
    private static List<UserRecord> readCSV(String fileName) {
        List<UserRecord> userList = new ArrayList<>();
        try (CSVParser csvParser = new CSVParser(new FileReader(fileName), CSVFormat.DEFAULT.withHeader())) {
            for (CSVRecord record : csvParser) {
                String userId = record.get("user_id");
                String email = record.isMapped("email") ? record.get("email") : "";
                String firstName = record.isMapped("first_name") ? record.get("first_name") : "";
                String lastName = record.isMapped("last_name") ? record.get("last_name") : "";
                userList.add(new UserRecord(userId, email, firstName, lastName));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return userList;
    }

    // Merge two lists of UserRecord objects
    private static List<UserRecord> mergeLists(List<UserRecord> listA, List<UserRecord> listB) {
        List<UserRecord> mergedList = new ArrayList<>();
        for (UserRecord userA : listA) {
            for (UserRecord userB : listB) {
                if (userA.getUserId().equals(userB.getUserId())) {
                    // Use the user_id from listB if it exists, otherwise use user_id from listA
                    String userId = userB.getUserId() != null ? userB.getUserId() : userA.getUserId();
                    mergedList.add(new UserRecord(userId, userA.getEmail(), userB.getFirstName(), userB.getLastName()));
                }
            }
        }
        return mergedList;
    }

    // Make an API request
    private static String makeApiRequest() {
        OkHttpClient client = new OkHttpClient();
        String domain = "sandbox.piano.io/api/v3";
        String aid = "o1sRRZSLlw";
        String apiToken = "xeYjNEhmutkgkqCZyhBn6DErVntAKDx30FqFOS6D";
        String endpoint = "publisher/user/list";
        String url = String.format("https://%s/%s?aid=%s&api_token=%s", domain, endpoint, aid, apiToken);

        Request request = new Request.Builder()
                .url(url)
                //.post(null)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                return response.body().string();
            } else {
                System.out.println("Request error. Status code: " + response.code());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    // Convert JSON response to a user matrix
    private static List<UserRecord> jsonToUserMatrix(String apiResponse) {
        List<UserRecord> userMatrix = new ArrayList<>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(apiResponse);
            JsonNode usersNode = rootNode.get("users");
            if (usersNode != null) {
                for (JsonNode userNode : usersNode) {
                    String email = userNode.get("email").asText();
                    String firstName = userNode.path("first_name").asText();
                    String lastName = userNode.path("last_name").asText();
                    String userId = userNode.path("uid").asText();
                    userMatrix.add(new UserRecord(userId, email, firstName, lastName));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return userMatrix;
    }

    // Update UIDs in the merged list
    private static void updateUidsInMergedList(List<UserRecord> mergedList, List<UserRecord> userMatrix) {
        for (UserRecord mergedUser : mergedList) {
            for (UserRecord userRow : userMatrix) {
                if (userRow.getEmail().equals(mergedUser.getEmail()) && userRow.getUserId() != null) {
                    mergedUser.setUserId(userRow.getUserId());
                }
            }
        }
    }

    // Save the list to a CSV file
    private static void saveToCSV(List<UserRecord> userList, String fileName) {
        try (CSVPrinter csvPrinter = CSVFormat.DEFAULT
                .withHeader("user_id", "email", "first_name", "last_name")
                .print(new FileWriter(fileName))) {
            for (UserRecord user : userList) {
                csvPrinter.printRecord(user.getUserId(), user.getEmail(), user.getFirstName(), user.getLastName());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void printUserList(List<UserRecord> userList) {
        for (UserRecord user : userList) {
            System.out.printf("%s %s %s %s%n", user.getUserId(), user.getEmail(), user.getFirstName(), user.getLastName());
        }
    }
}

class UserRecord {
    private String userId;
    private String email;
    private String firstName;
    private String lastName;

    public UserRecord(String userId, String email, String firstName, String lastName) {
        this.userId = userId;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }
}
