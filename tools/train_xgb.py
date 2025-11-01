import numpy as np
from sklearn.model_selection import train_test_split
from xgboost import XGBClassifier

rng = np.random.default_rng(42)
N = 20000

amountZ = rng.normal(0, 1, N)
window15 = rng.poisson(2, N)
first = (rng.random(N) < 0.1).astype(int)
amount = np.exp(rng.normal(12, 1, N))

X = np.vstack([np.log1p(amount), amountZ, window15, first]).T

y = ((amountZ > 3.0) & (window15 > 10) | (first & (np.log1p(amount) > 14))).astype(int)

Xtr, Xte, ytr, yte = train_test_split(X, y, test_size=0.2, random_state=13)
Xtr = Xtr.astype("float32")
Xte = Xte.astype("float32")
ytr = ytr.astype("float32")
yte = yte.astype("float32")

clf = XGBClassifier(
    n_estimators=200,
    max_depth=6,
    learning_rate=0.1,
    subsample=0.9,
    colsample_bytree=0.8,
    reg_lambda=1.0,
    n_jobs=4,
)
clf.fit(Xtr, ytr, eval_set=[(Xte, yte)], verbose=False)
clf.get_booster().save_model("model.xgb")
print("wrote model.xgb")
