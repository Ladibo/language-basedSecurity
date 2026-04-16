import backEnd.*;
import java.util.Scanner;

public class ShoppingCart {
    private static void print(Wallet wallet, Pocket pocket) throws Exception {
        System.out.println("Your current balance is: " + wallet.getBalance() + " credits.");
        System.out.println(Store.asString());
        System.out.println("Your current pocket is:\n" + pocket.getPocket());
    }

    private static String scan(Scanner scanner) throws Exception {
        System.out.print("What do you want to buy? (type quit to stop) ");
        return scanner.nextLine();
    }

    public static void main(String[] args) throws Exception {
        Wallet wallet = new Wallet();
        Pocket pocket = new Pocket();
        Scanner scanner = new Scanner(System.in);

        print(wallet, pocket);
        String product = scan(scanner);

        while(!product.equals("quit")) {
            int price;
            try {
                price = Store.getProductPrice(product);
            } catch (IllegalArgumentException e) {
                System.out.println("Product not found: " + product);
                print(wallet, pocket);
                product = scan(scanner);
                continue;
            }

            if (wallet.getBalance() < price) {
                System.out.println("Not enough credits.");
                break;
            }

            wallet.setBalance(wallet.getBalance() - price);
            pocket.addProduct(product);

            print(wallet, pocket);
            product = scan(scanner);
        }
    }
}
