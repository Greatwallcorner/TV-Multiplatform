
def main():
    do = increase()
    print(do())
    print(do())
    print(do())

def log(func):
    def wrapper(*arg, **kw):
        print("call you ", func.__name__)
        return func(*arg, **kw)
    return wrapper

@log
def increase():
    x = 1
    def do():
        nonlocal x 
        x = x + 1
        return x
    return do

    


if __name__ == "__main__":
    main()