
import java.math.BigDecimal;
import java.math.RoundingMode;

enum Currency {
    RUB("RUB", "₽", 100),
    USD("USD", "$", 9600),
    EUR("EUR", "€", 10300),
    CNY("CNY", "¥", 1320);

    private final String code;
    private final String symbol;
    private final long priceInRublesKopecks;

    Currency(String code, String symbol, long priceInRublesKopecks) {
        this.code = code;
        this.symbol = symbol;
        this.priceInRublesKopecks = priceInRublesKopecks;
    }

    public String getCode() {
        return code;
    }

    public String getSymbol() {
        return symbol;
    }

    public long getPriceInRublesKopecks() {
        return priceInRublesKopecks;
    }
}

final class Balance {
    private final long amount;
    private final Currency currency;

    public Balance(long amount, Currency currency) {
        if (amount < 0 || currency == null) {
            throw new IllegalArgumentException("Неверный баланс");
        }

        this.amount = amount;
        this.currency = currency;
    }

    public long getAmount() {
        return amount;
    }

    public Currency getCurrency() {
        return currency;
    }

    public Balance add(Balance other) {
        checkCurrency(other);

        return new Balance(Math.addExact(amount, other.amount), currency);
    }

    public Balance subtract(Balance other) {
        checkCurrency(other);

        if (amount < other.amount) {
            throw new IllegalArgumentException("Недостаточно денег");
        }

        return new Balance(amount - other.amount, currency);
    }

    public Balance convertTo(Currency newCurrency) {
        if (newCurrency == null) {
            throw new IllegalArgumentException("Валюта не указана");
        }

        BigDecimal result = BigDecimal.valueOf(amount)
                .multiply(BigDecimal.valueOf(currency.getPriceInRublesKopecks()))
                .divide(
                        BigDecimal.valueOf(newCurrency.getPriceInRublesKopecks()),
                        0,
                        RoundingMode.HALF_UP
                );

        return new Balance(result.longValueExact(), newCurrency);
    }

    private void checkCurrency(Balance other) {
        if (other == null || currency != other.currency) {
            throw new IllegalArgumentException("Разные валюты");
        }
    }

    @Override
    public String toString() {
        return String.format(
                "%d.%02d %s",
                amount / 100,
                amount % 100,
                currency.getSymbol()
        );
    }
}

class BankAccount {
    private final String number;
    private final String owner;
    private final long limit;

    private Balance balance;

    public BankAccount(String number, String owner,
                       Balance balance, long limit) {

        if (number == null || number.isBlank()
                || owner == null || owner.isBlank()
                || balance == null || limit < 0) {
            throw new IllegalArgumentException("Неверные данные счёта");
        }

        this.number = number;
        this.owner = owner;
        this.balance = balance;
        this.limit = limit;
    }

    public Balance getBalance() {
        return balance;
    }

    public void deposit(Balance money) {
        if (money == null || money.getAmount() <= 0) {
            throw new IllegalArgumentException("Неверная сумма");
        }

        Balance converted = money.convertTo(balance.getCurrency());
        balance = balance.add(converted);
    }

    public void withdraw(long amount) {
        checkLimit(amount);

        Balance money = new Balance(amount, balance.getCurrency());
        balance = balance.subtract(money);
    }

    public void transferTo(BankAccount account, long amount) {
        if (account == null || account == this) {
            throw new IllegalArgumentException("Неверный счёт");
        }

        checkLimit(amount);

        Balance money = new Balance(amount, balance.getCurrency());
        Balance received = money.convertTo(account.balance.getCurrency());

        Balance newBalance = balance.subtract(money);
        Balance newAccountBalance = account.balance.add(received);

        balance = newBalance;
        account.balance = newAccountBalance;
    }

    private void checkLimit(long amount) {
        if (amount <= 0 || amount > limit) {
            throw new IllegalArgumentException("Превышен лимит или неверная сумма");
        }
    }

    @Override
    public String toString() {
        return "Владелец: " + owner
                + ", счёт: " + number
                + ", баланс: " + balance;
    }
}

public class Main {
    public static void main(String[] args) {

        BankAccount account1 = new BankAccount(
                "001",
                "Иван",
                new Balance(100_000, Currency.RUB),
                50_000
        );

        BankAccount account2 = new BankAccount(
                "002",
                "Анна",
                new Balance(50_000, Currency.USD),
                20_000
        );

        System.out.println("До операций:");
        System.out.println(account1);
        System.out.println(account2);

        account1.deposit(new Balance(10_000, Currency.RUB));

        account1.withdraw(5_000);

        account1.transferTo(account2, 10_000);

        System.out.println("\nПосле операций:");
        System.out.println(account1);
        System.out.println(account2);

        Balance money = new Balance(10_000, Currency.RUB);

        System.out.println("\nКонвертация:");
        System.out.println(money.convertTo(Currency.USD));
    }
}
